package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.client.BankHolidayDataClient
import uk.gov.dluhc.printapi.client.BankHolidayDivision.ENGLAND_AND_WALES
import uk.gov.dluhc.printapi.config.DataRetentionConfiguration
import java.time.LocalDate
import java.time.Period

@ExtendWith(MockitoExtension::class)
internal class ElectorDocumentRemovalDateResolverTest {
    @Mock
    private lateinit var dataRetentionConfig: DataRetentionConfiguration

    @Mock
    private lateinit var bankHolidayDataClient: BankHolidayDataClient

    @InjectMocks
    private lateinit var electorDocumentRemovalDateResolver: ElectorDocumentRemovalDateResolver

    @Nested
    inner class CertificateInitialRetentionPeriod {
        @Test
        fun `should get initial retention period removal date for delivery info given upcoming bank holiday`() {
            // Given
            val gssCode = "E09000007"
            val issueDate = LocalDate.of(2023, 1, 1)
            val upcomingBankHoliday = LocalDate.of(2023, 2, 1)
            val expectedRemovalDate = LocalDate.of(2023, 2, 9)
            given(dataRetentionConfig.certificateInitialRetentionPeriod).willReturn(Period.ofDays(28))
            given(bankHolidayDataClient.getBankHolidayDates(any(), any(), any())).willReturn(listOf(upcomingBankHoliday))

            // When
            val actual = electorDocumentRemovalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(issueDate, gssCode)

            // Then
            assertThat(actual).isEqualTo(expectedRemovalDate)
            verify(dataRetentionConfig).certificateInitialRetentionPeriod
            verify(bankHolidayDataClient).getBankHolidayDates(ENGLAND_AND_WALES)
        }

        @Test
        fun `should get initial retention period removal date for delivery info given no upcoming bank holidays`() {
            // Given
            val gssCode = "E09000007"
            val issueDate = LocalDate.of(2023, 1, 1)
            val expectedRemovalDate = LocalDate.of(2023, 2, 8)
            given(dataRetentionConfig.certificateInitialRetentionPeriod).willReturn(Period.ofDays(28))
            given(bankHolidayDataClient.getBankHolidayDates(any(), any(), any())).willReturn(emptyList())

            // When
            val actual = electorDocumentRemovalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(issueDate, gssCode)

            // Then
            assertThat(actual).isEqualTo(expectedRemovalDate)
            verify(dataRetentionConfig).certificateInitialRetentionPeriod
            verify(bankHolidayDataClient).getBankHolidayDates(ENGLAND_AND_WALES)
        }
    }

    @Nested
    inner class ElectorDocumentFinalRetentionPeriod {
        @ParameterizedTest
        @CsvSource(
            "2023-01-01, 2032-07-01",
            "2023-06-30, 2032-07-01",
            "2023-07-01, 2033-07-01",
            "2023-07-02, 2033-07-01",
            "2024-01-01, 2033-07-01",
            "2024-12-31, 2034-07-01"
        )
        fun `should get final retention period removal date for elector document`(issueDate: LocalDate, expectedTargetDate: LocalDate) {
            // Given

            // When
            val actualTargetDate = electorDocumentRemovalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(issueDate)

            // Then
            assertThat(actualTargetDate).isEqualTo(expectedTargetDate)
        }
    }

    @Nested
    inner class TemporaryCertificateFinalRetentionPeriod {
        @ParameterizedTest
        @CsvSource(
            "2023-01-01, 2024-07-01",
            "2023-06-30, 2024-07-01",
            "2023-07-01, 2025-07-01",
            "2023-07-02, 2025-07-01",
            "2024-01-01, 2025-07-01",
            "2024-12-31, 2026-07-01"
        )
        fun `should get final retention period removal date for temporary certificate`(issueDate: LocalDate, expectedTargetDate: LocalDate) {
            // Given

            // When
            val actualTargetDate = electorDocumentRemovalDateResolver.getTempCertFinalRetentionPeriodRemovalDate(issueDate)

            // Then
            assertThat(actualTargetDate).isEqualTo(expectedTargetDate)
        }
    }
}
