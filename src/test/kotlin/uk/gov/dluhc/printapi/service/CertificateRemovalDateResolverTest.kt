package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.client.BankHolidayDataClient
import uk.gov.dluhc.printapi.client.BankHolidayDivision.ENGLAND_AND_WALES
import uk.gov.dluhc.printapi.config.DataRetentionConfiguration
import java.time.Duration
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class CertificateRemovalDateResolverTest {
    @Mock
    private lateinit var dataRetentionConfig: DataRetentionConfiguration

    @Mock
    private lateinit var bankHolidayDataClient: BankHolidayDataClient

    @InjectMocks
    private lateinit var certificateRemovalDateResolver: CertificateRemovalDateResolver

    @Test
    fun `should get removal date for delivery info given upcoming bank holiday`() {
        // Given
        val gssCode = "E09000007"
        val issueDate = LocalDate.of(2023, 1, 1)
        val upcomingBankHoliday = LocalDate.of(2023, 2, 1)
        val expectedRemovalDate = LocalDate.of(2023, 2, 9)
        given(dataRetentionConfig.certificateDeliveryInfo).willReturn(Duration.ofDays(28))
        given(bankHolidayDataClient.getBankHolidayDates(any(), any(), any())).willReturn(listOf(upcomingBankHoliday))

        // When
        val actual = certificateRemovalDateResolver.getCertificateDeliveryInfoRemovalDate(issueDate, gssCode)

        // Then
        assertThat(actual).isEqualTo(expectedRemovalDate)
        verify(dataRetentionConfig).certificateDeliveryInfo
        verify(bankHolidayDataClient).getBankHolidayDates(ENGLAND_AND_WALES)
    }

    @Test
    fun `should get removal date for delivery info given no upcoming bank holidays`() {
        // Given
        val gssCode = "E09000007"
        val issueDate = LocalDate.of(2023, 1, 1)
        val expectedRemovalDate = LocalDate.of(2023, 2, 8)
        given(dataRetentionConfig.certificateDeliveryInfo).willReturn(Duration.ofDays(28))
        given(bankHolidayDataClient.getBankHolidayDates(any(), any(), any())).willReturn(emptyList())

        // When
        val actual = certificateRemovalDateResolver.getCertificateDeliveryInfoRemovalDate(issueDate, gssCode)

        // Then
        assertThat(actual).isEqualTo(expectedRemovalDate)
        verify(dataRetentionConfig).certificateDeliveryInfo
        verify(bankHolidayDataClient).getBankHolidayDates(ENGLAND_AND_WALES)
    }
}
