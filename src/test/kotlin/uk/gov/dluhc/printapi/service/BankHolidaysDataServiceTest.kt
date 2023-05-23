package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.bankholidaysdataclient.BankHolidayDataClient
import uk.gov.dluhc.bankholidaysdataclient.BankHolidayDivision
import uk.gov.dluhc.printapi.testsupport.toRoundedUTCOffsetDateTime
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
internal class BankHolidaysDataServiceTest {

    private lateinit var bankHolidaysDataService: BankHolidaysDataService

    @Mock
    private lateinit var bankHolidayDataClient: BankHolidayDataClient

    private val fixedDateAndTime = LocalDate.of(2023, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()

    private val fixedClock = Clock.fixed(fixedDateAndTime, ZoneOffset.UTC)

    @BeforeEach
    fun beforeEach() {
        bankHolidaysDataService = BankHolidaysDataService(bankHolidayDataClient, fixedClock)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "N123456781,NORTHERN_IRELAND",
            "E123456782,ENGLAND_AND_WALES",
            "W123456783,ENGLAND_AND_WALES",
            "S123456784,SCOTLAND"
        ]
    )
    fun `should get bank holiday dates for given bank holiday division`(
        gssCode: String,
        expectedBankHolidayDivision: BankHolidayDivision
    ) {
        // Given
        val expectedBankHolidayDatesResponse = listOf<LocalDate>(LocalDate.now().plusDays(25))
        val today: LocalDate = fixedDateAndTime.toRoundedUTCOffsetDateTime().toLocalDate()
        given(bankHolidayDataClient.getBankHolidayDates(any(), any(), any())).willReturn(
            expectedBankHolidayDatesResponse
        )

        // When
        val actualBankHolidayDates = bankHolidaysDataService.getUpcomingBankHolidays(gssCode)

        // Then
        assertThat(actualBankHolidayDates).hasSize(1).isEqualTo(expectedBankHolidayDatesResponse)
        verify(bankHolidayDataClient).getBankHolidayDates(expectedBankHolidayDivision, today, today.plusDays(100))
    }
}
