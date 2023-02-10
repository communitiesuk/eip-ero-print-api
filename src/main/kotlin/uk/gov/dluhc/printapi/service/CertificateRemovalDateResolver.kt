package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.client.BankHolidayDataClient
import uk.gov.dluhc.printapi.client.BankHolidayDivision
import uk.gov.dluhc.printapi.config.DataRetentionConfiguration
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Responsible for determining the removal date for a Certificate's data. The removal date varies, depending on the
 * category of data and the type of Certificate concerned.
 */
@Component
class CertificateRemovalDateResolver(
    private val dataRetentionConfig: DataRetentionConfiguration,
    private val bankHolidayDataClient: BankHolidayDataClient
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
    fun getCertificateInitialRetentionPeriodRemovalDate(issueDate: LocalDate, gssCode: String): LocalDate =
        with(getTotalDaysForWorkingDays(issueDate, dataRetentionConfig.certificateInitialRetentionPeriod.days, gssCode)) {
            issueDate.plusDays(this.toLong())
        }

    private fun getTotalDaysForWorkingDays(issueDate: LocalDate, requiredWorkingDays: Int, gssCode: String): Int {
        val upcomingBankHolidays = bankHolidayDataClient.getBankHolidayDates(BankHolidayDivision.fromGssCode(gssCode))
        var date = issueDate
        var workingDays = 0
        while (workingDays < requiredWorkingDays) {
            date = date.plusDays(1)
            if (isWorkingDay(date, upcomingBankHolidays)) {
                workingDays++
            }
        }
        return ChronoUnit.DAYS.between(issueDate, date).toInt()
    }

    private fun isWorkingDay(date: LocalDate, upcomingBankHolidays: List<LocalDate>) =
        !isWeekend(date) && !isBankHoliday(upcomingBankHolidays, date)

    private fun isWeekend(date: LocalDate) = date.dayOfWeek == SATURDAY || date.dayOfWeek == SUNDAY

    private fun isBankHoliday(upcomingBankHolidays: List<LocalDate>, date: LocalDate) =
        upcomingBankHolidays.contains(date)
}
