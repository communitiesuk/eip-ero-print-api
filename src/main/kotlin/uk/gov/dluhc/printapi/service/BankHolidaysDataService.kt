package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import uk.gov.dluhc.bankholidaysdataclient.BankHolidayDataClient
import uk.gov.dluhc.bankholidaysdataclient.BankHolidayDivision
import uk.gov.dluhc.printapi.config.UK_BANK_HOLIDAYS_CACHE
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

/**
 * Service class that uses internal dluhc library client which interacts with S3 bank holidays bucket.
 * Uses v2 version of AWS S3 BankHolidayDataClient.
 * Note: The service method that calculates bank holidays is cached.
 */
@Service
class BankHolidaysDataService(
    private val bankHolidayDataClient: BankHolidayDataClient,
) {

    @Cacheable(UK_BANK_HOLIDAYS_CACHE)
    fun getUpcomingBankHolidays(
        gssCode: String,
        fromDate: LocalDate,
        toDate: LocalDate = fromDate.plusDays(100)
    ): List<LocalDate> {
        logger.info { "Computing bank holiday(s) between [$fromDate] and [$toDate]" }
        return bankHolidayDataClient.getBankHolidayDates(
            division = BankHolidayDivision.fromGssCode(gssCode),
            fromDate = fromDate,
            toDate = toDate
        ).also { bankHolidays ->
            logger.info { "Upcoming ${bankHolidays.size} bank holiday(s) between [$fromDate] and [$toDate] on $bankHolidays" }
        }
    }
}
