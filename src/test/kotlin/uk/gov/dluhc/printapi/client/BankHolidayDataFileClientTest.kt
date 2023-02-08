package uk.gov.dluhc.printapi.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.client.BankHolidayDivision.ENGLAND_AND_WALES
import uk.gov.dluhc.printapi.config.IntegrationTest
import java.time.LocalDate

internal class BankHolidayDataFileClientTest : IntegrationTest() {

    @ParameterizedTest
    @CsvSource(
        value = [
            "2023-01-01,2023-12-31,9",
            "2023-01-01,2023-01-31,1",
            "2023-01-03,2023-01-31,0",
            "2023-04-03,2023-04-11,2"
        ]
    )
    fun `should get bank holiday dates`(fromDate: LocalDate, toDate: LocalDate, count: Int) {
        // Given

        // When
        val bankHolidayDates = bankHolidayDataClient.getBankHolidayDates(ENGLAND_AND_WALES, fromDate, toDate)

        // Then
        assertThat(bankHolidayDates).hasSize(count)
    }
}
