package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.config.DataRetentionConfiguration
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit.DAYS

/**
 * Responsible for determining the removal date for an Elector Document's data. The removal date varies, depending on
 * the category of data and the type of Elector Document concerned.
 */
@Component
class ElectorDocumentRemovalDateResolver(
    private val dataRetentionConfig: DataRetentionConfiguration,
    private val bankHolidaysDataService: BankHolidaysDataService,
) {

    /**
     * Calculates the date that certain [uk.gov.dluhc.printapi.database.entity.Certificate] related data should be
     * removed following the first retention period. The legislation stipulates that PII data which is not on the
     * printed certificate should be removed 28 (configurable) working days from the certificate's "issue" date. This
     * means that weekends and bank holidays should be excluded when making this calculation. Note that if the issue
     * date was more than 28 working days ago, the removal date will be in the past.
     *
     * Due to the PII data in question (currently just address related information), this method is only relevant to
     * standard VCA certificates, not temporary certificates or AEDs.
     *
     * @param issueDate The date the Certificate was issued.
     * @param gssCode The Certificate's GSS code which is used to determine if there are any upcoming bank holidays for
     * the UK nation concerned.
     * @return A [LocalDate] representing when the data should be removed
     */
    fun getCertificateInitialRetentionPeriodRemovalDate(
        issueDate: LocalDate,
        gssCode: String,
        isCertificateCreatedWithPrinterProvidedIssueDate: Boolean,
    ): LocalDate {
        // This is a temporary workaround to ensure that applications created before EROPSPT-418 is deployed
        // are retained for 29 (1 extra) working days.
        // TODO EROPSPT-XXX: Remove this workaround once all applications created before EROPSPT-418 are removed
        val requiredWorkingDays = if (isCertificateCreatedWithPrinterProvidedIssueDate) {
            dataRetentionConfig.certificateInitialRetentionPeriod.days
        } else {
            dataRetentionConfig.legacyCertificateInitialRetentionPeriod.days
        }

        val daysToAdd = getTotalDaysForWorkingDays(
            issueDate,
            requiredWorkingDays,
            gssCode,
        )

        return issueDate.plusDays(daysToAdd.toLong())
    }

    /**
     * Calculates the date that certain [uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument] related data
     * should be removed following the first retention period. The legislation stipulates that PII data which is not on
     * the printed document should be removed 15 months after it's issue date.
     *
     * @param issueDate The date the Anonymous Elector Document was generated (and assumed to be printed).
     * @return A [LocalDate] representing when the data should be removed.
     */
    fun getAedInitialRetentionPeriodRemovalDate(issueDate: LocalDate): LocalDate {
        return issueDate.plusMonths(15)
    }

    /**
     * Calculates the date that any remaining "Elector Document" data should be removed following the final retention
     * period. The legislation stipulates that all remaining data should be removed on the tenth 1st July following the
     * date the VAC's issue date.
     *
     * "Elector Document" here means either a [uk.gov.dluhc.printapi.database.entity.Certificate] or a
     * [uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument]). but does not include
     * [uk.gov.dluhc.printapi.database.entity.TemporaryCertificate], which has a different retention period.
     * @see getTempCertFinalRetentionPeriodRemovalDate
     *
     * @param issueDate The date the Elector Document was issued.
     * @return A [LocalDate] representing when the data should be removed
     */
    fun getElectorDocumentFinalRetentionPeriodRemovalDate(issueDate: LocalDate): LocalDate {
        val firstJuly = LocalDate.of(issueDate.year, Month.JULY, 1)
        val numberOfYears =
            when (issueDate.isBefore(firstJuly)) {
                true -> 9L
                false -> 10L
            }
        return firstJuly.plusYears(numberOfYears)
    }

    /**
     * Calculates the date that a [uk.gov.dluhc.printapi.database.entity.TemporaryCertificate] should be removed, which
     * the legislation specifies as the second 1st July following the temporary certificate's issue date.
     *
     * @param issueDate The date the [uk.gov.dluhc.printapi.database.entity.TemporaryCertificate] was issued.
     * @return A [LocalDate] representing when the data should be removed
     */
    fun getTempCertFinalRetentionPeriodRemovalDate(issueDate: LocalDate): LocalDate? {
        val firstJuly = LocalDate.of(issueDate.year, Month.JULY, 1)
        val numberOfYears =
            when (issueDate.isBefore(firstJuly)) {
                true -> 1L
                false -> 2L
            }
        return firstJuly.plusYears(numberOfYears)
    }

    private fun getTotalDaysForWorkingDays(issueDate: LocalDate, requiredWorkingDays: Int, gssCode: String): Int {
        val upcomingBankHolidays =
            bankHolidaysDataService.getUpcomingBankHolidays(gssCode = gssCode, fromDate = issueDate)

        var date = issueDate
        var workingDays = 0
        while (workingDays < requiredWorkingDays) {
            date = date.plusDays(1)
            if (isWorkingDay(date, upcomingBankHolidays)) {
                workingDays++
            }
        }
        return DAYS.between(issueDate, date).toInt()
    }

    private fun isWorkingDay(date: LocalDate, upcomingBankHolidays: List<LocalDate>) =
        !isWeekend(date) && !isBankHoliday(upcomingBankHolidays, date)

    private fun isWeekend(date: LocalDate) = date.dayOfWeek == SATURDAY || date.dayOfWeek == SUNDAY

    private fun isBankHoliday(upcomingBankHolidays: List<LocalDate>, date: LocalDate) =
        upcomingBankHolidays.contains(date)
}
