package uk.gov.dluhc.printapi.service.temporarycertificate

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
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateSummaryMapper
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildTemporaryCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate

@ExtendWith(MockitoExtension::class)
class TemporaryCertificateSummaryServiceTest {

    @Mock
    private lateinit var temporaryCertificateFinderService: TemporaryCertificateFinderService

    @Mock
    private lateinit var temporaryCertificateSummaryMapper: TemporaryCertificateSummaryMapper

    @InjectMocks
    private lateinit var temporaryCertificateSummaryService: TemporaryCertificateSummaryService

    @Test
    fun `should get Temporary Certificate Summaries given an application with some Temporary Certificates`() {
        // Given
        val eroId = aValidEroId()
        val sourceType = aValidSourceType()
        val sourceReference = aValidSourceReference()

        val firstTemporaryCertificate = buildTemporaryCertificate()
        val secondTemporaryCertificate = buildTemporaryCertificate()
        val temporaryCertificates = listOf(firstTemporaryCertificate, secondTemporaryCertificate)
        given(temporaryCertificateFinderService.getTemporaryCertificates(any(), any(), any())).willReturn(temporaryCertificates)

        val firstTemporaryCertificateSummary = buildTemporaryCertificateSummaryDto()
        val secondTemporaryCertificateSummary = buildTemporaryCertificateSummaryDto()
        given(temporaryCertificateSummaryMapper.toDtoTemporaryCertificateSummary(any())).willReturn(
            firstTemporaryCertificateSummary,
            secondTemporaryCertificateSummary
        )

        val expected = listOf(firstTemporaryCertificateSummary, secondTemporaryCertificateSummary)

        // When
        val actual = temporaryCertificateSummaryService.getTemporaryCertificateSummaries(eroId, sourceType, sourceReference)

        // Then
        verify(temporaryCertificateFinderService).getTemporaryCertificates(eroId, sourceType, sourceReference)
        verify(temporaryCertificateSummaryMapper).toDtoTemporaryCertificateSummary(firstTemporaryCertificate)
        verify(temporaryCertificateSummaryMapper).toDtoTemporaryCertificateSummary(secondTemporaryCertificate)
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }

    @Test
    fun `should get Temporary Certificate Summaries given an application with no Temporary Certificates`() {
        // Given
        val eroId = aValidEroId()
        val sourceType = aValidSourceType()
        val sourceReference = aValidSourceReference()

        given(temporaryCertificateFinderService.getTemporaryCertificates(any(), any(), any())).willReturn(emptyList())

        // When
        val actual = temporaryCertificateSummaryService.getTemporaryCertificateSummaries(eroId, sourceType, sourceReference)

        // Then
        verify(temporaryCertificateFinderService).getTemporaryCertificates(eroId, sourceType, sourceReference)
        verifyNoInteractions(temporaryCertificateSummaryMapper)
        assertThat(actual).isEmpty()
    }
}
