package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.exception.CertificateNotFoundException
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCodeList

@ExtendWith(MockitoExtension::class)
internal class CertificateFinderServiceTest {

    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @InjectMocks
    private lateinit var certificateFinderService: CertificateFinderService

    @Nested
    inner class GetCertificate {

        @Test
        fun `should get Certificate as one exists for the provided details including ERO identifier`() {
            // Given
            val eroId = aValidRandomEroId()
            val sourceType = VOTER_CARD
            val sourceReference = aValidSourceReference()

            val gssCodes = getRandomGssCodeList()
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            val certificate = buildCertificate()
            given(certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(certificate)

            // When
            val actual = certificateFinderService.getCertificate(eroId, sourceType, sourceReference)

            // Then
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(certificateRepository).findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, sourceType, sourceReference)
            assertThat(actual).isSameAs(certificate)
        }

        @Test
        fun `should raise exception as no certificate exists for the provided details including ERO identifier`() {
            // Given
            val eroId = "camden-city-council"
            val sourceType = VOTER_CARD
            val sourceReference = "63774ff4bb4e7049b67182d9"

            val gssCodes = getRandomGssCodeList()
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(null)

            // When
            val error = catchThrowableOfType(CertificateNotFoundException::class.java) {
                certificateFinderService.getCertificate(eroId, sourceType, sourceReference)
            }

            // Then
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(certificateRepository).findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, sourceType, sourceReference)
            assertThat(error)
                .isNotNull
                .hasMessage("Certificate for eroId = camden-city-council with sourceType = VOTER_CARD and sourceReference = 63774ff4bb4e7049b67182d9 not found")
        }

        @Test
        fun `should get Certificate as one exists for the provided details excluding ERO identifier`() {
            // Given
            val sourceType = VOTER_CARD
            val sourceReference = aValidSourceReference()

            val certificate = buildCertificate()
            given(certificateRepository.findBySourceTypeAndSourceReference(any(), any())).willReturn(certificate)

            // When
            val actual = certificateFinderService.getCertificate(sourceType, sourceReference)

            // Then
            verify(certificateRepository).findBySourceTypeAndSourceReference(sourceType, sourceReference)
            assertThat(actual).isSameAs(certificate)
        }

        @Test
        fun `should raise exception as no certificate exists for the provided details excluding ERO identifier`() {
            // Given
            val sourceType = VOTER_CARD
            val sourceReference = "63774ff4bb4e7049b67182d9"

            given(certificateRepository.findBySourceTypeAndSourceReference(any(), any())).willReturn(null)

            // When
            val error = catchThrowableOfType(CertificateNotFoundException::class.java) {
                certificateFinderService.getCertificate(sourceType, sourceReference)
            }

            // Then
            verify(certificateRepository).findBySourceTypeAndSourceReference(sourceType, sourceReference)
            assertThat(error)
                .isNotNull
                .hasMessage("Certificate with sourceType = VOTER_CARD and sourceReference = 63774ff4bb4e7049b67182d9 not found")
        }
    }
}
