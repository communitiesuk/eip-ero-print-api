package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidOnDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildTemporaryCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildTemporaryCertificateSummary
import java.time.Instant
import uk.gov.dluhc.printapi.dto.TemporaryCertificateStatusDto as TemporaryCertificateStatusDto
import uk.gov.dluhc.printapi.models.TemporaryCertificateStatus as TemporaryCertificateStatusApi

@ExtendWith(MockitoExtension::class)
class TemporaryCertificateSummaryMapperTest {

    @Mock
    private lateinit var instantMapper: InstantMapper

    @InjectMocks
    private lateinit var mapper: TemporaryCertificateSummaryMapperImpl

    @Test
    fun `should map DTO to api model`() {
        // Given
        val certificateNumber = aValidCertificateNumber()
        val userId = aValidUserId()
        val dateTimeGenerated = aValidGeneratedDateTime()
        val issueDate = aValidIssueDate()
        val validOnDate = aValidOnDate()

        val dto = buildTemporaryCertificateSummaryDto(
            certificateNumber = certificateNumber,
            userId = userId,
            dateTimeGenerated = dateTimeGenerated,
            issueDate = issueDate,
            validOnDate = validOnDate,
            status = TemporaryCertificateStatusDto.GENERATED
        )
        val expected = buildTemporaryCertificateSummary(
            certificateNumber = certificateNumber,
            userId = userId,
            dateTimeGenerated = dateTimeGenerated,
            issueDate = issueDate,
            validOnDate = validOnDate,
            status = TemporaryCertificateStatusApi.GENERATED
        )

        // When
        val actual = mapper.toApiTemporaryCertificateSummary(dto)

        // Then
        assertThat(actual).isEqualTo(expected)
        verifyNoInteractions(instantMapper)
    }

    @Test
    fun `should map entity to DTO`() {
        // Given
        val certificateNumber = aValidCertificateNumber()
        val userId = aValidUserId()
        val issueDate = aValidIssueDate()
        val validOnDate = aValidOnDate()

        val dateTimeGenerated = Instant.now()
        val statusHistory = listOf(
            buildTemporaryCertificateStatus(
                status = TemporaryCertificateStatus.Status.GENERATED,
                dateCreated = dateTimeGenerated
            )
        )
        val entity = buildTemporaryCertificate(
            certificateNumber = certificateNumber,
            userId = userId,
            issueDate = issueDate,
            validOnDate = validOnDate,
            statusHistory = statusHistory
        )

        val expectedDateTimeGenerated = aValidGeneratedDateTime()
        given(instantMapper.toOffsetDateTime(any())).willReturn(expectedDateTimeGenerated)
        val expected = buildTemporaryCertificateSummaryDto(
            certificateNumber = certificateNumber,
            userId = userId,
            issueDate = issueDate,
            validOnDate = validOnDate,
            status = TemporaryCertificateStatusDto.GENERATED,
            dateTimeGenerated = expectedDateTimeGenerated
        )

        // When
        val actual = mapper.toDtoTemporaryCertificateSummary(entity)

        // Then
        verify(instantMapper).toOffsetDateTime(dateTimeGenerated)
        assertThat(actual).isEqualTo(expected)
    }
}
