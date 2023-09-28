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
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCodeList

@ExtendWith(MockitoExtension::class)
internal class TemporaryCertificateFinderServiceTest {

    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var temporaryCertificateRepository: TemporaryCertificateRepository

    @InjectMocks
    private lateinit var temporaryCertificateFinderService: TemporaryCertificateFinderService

    @Test
    fun `should get TemporaryCertificates given the provided details including an ERO identifier`() {
        // Given
        val eroId = aValidRandomEroId()
        val sourceType = VOTER_CARD
        val sourceReference = aValidSourceReference()

        val gssCodes = getRandomGssCodeList()
        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
        val temporaryCertificates = listOf(buildTemporaryCertificate())
        given(
            temporaryCertificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(
                any(),
                any(),
                any()
            )
        ).willReturn(temporaryCertificates)

        // When
        val actual = temporaryCertificateFinderService.getTemporaryCertificates(eroId, sourceType, sourceReference)

        // Then
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(temporaryCertificateRepository).findByGssCodeInAndSourceTypeAndSourceReference(
            gssCodes,
            sourceType,
            sourceReference
        )
        assertThat(actual).isSameAs(temporaryCertificates)
    }

    @Test
    fun `should get TemporaryCertificates given the provided details excluding an ERO identifier`() {
        // Given
        val sourceType = VOTER_CARD
        val sourceReference = aValidSourceReference()

        val temporaryCertificates = listOf(buildTemporaryCertificate())
        given(
            temporaryCertificateRepository.findBySourceTypeAndSourceReference(any(), any())
        ).willReturn(temporaryCertificates)

        // When
        val actual = temporaryCertificateFinderService.getTemporaryCertificates(sourceType, sourceReference)

        // Then
        verify(temporaryCertificateRepository).findBySourceTypeAndSourceReference(
            sourceType,
            sourceReference
        )
        assertThat(actual).isSameAs(temporaryCertificates)
    }
}
