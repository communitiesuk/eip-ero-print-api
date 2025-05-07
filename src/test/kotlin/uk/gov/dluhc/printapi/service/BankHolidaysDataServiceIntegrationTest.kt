package uk.gov.dluhc.printapi.service

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import java.time.LocalDate

internal class BankHolidaysDataServiceIntegrationTest : IntegrationTest() {

    @Test
    fun `should cache bank holidays until ttl and evict cache after cache ttl has elapsed`() {
        // Given
        val gssCode = "E12345678"
        val fromDate = LocalDate.now()
        val toDate = fromDate.plusDays(100)
        val expectedLoggerMessage = "Computing bank holiday(s) between [$fromDate] and [$toDate]"

        // When
        bankHolidaysDataService.getUpcomingBankHolidays(gssCode = gssCode, fromDate = fromDate)

        // Then
        assertThat(TestLogAppender.hasLog(expectedLoggerMessage, Level.INFO)).isTrue

        TestLogAppender.reset()

        // Within the cache TTL, service must retrieve cached copy of bank holidays
        bankHolidaysDataService.getUpcomingBankHolidays(gssCode = gssCode, fromDate = fromDate)
        assertThat(TestLogAppender.hasLog(expectedLoggerMessage, Level.INFO)).isFalse

        TestLogAppender.reset()

        // Wait until cache TTL has elapsed
        Thread.sleep(timeToLive.toMillis())

        // After the cache TTL has elapsed, service must compute bank holidays again
        bankHolidaysDataService.getUpcomingBankHolidays(gssCode = gssCode, fromDate = fromDate)
        assertThat(TestLogAppender.hasLog(expectedLoggerMessage, Level.INFO)).isTrue
    }
}
