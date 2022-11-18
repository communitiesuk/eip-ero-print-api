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
import uk.gov.dluhc.printapi.database.mapper.CertificateSummaryDtoMapper
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate

@ExtendWith(MockitoExtension::class)
internal class CertificateSummaryServiceTest {

    @Mock
    lateinit var certificateFinderService: CertificateFinderService

    @Mock
    lateinit var mapper: CertificateSummaryDtoMapper

    @InjectMocks
    lateinit var certificateSummaryService: CertificateSummaryService

    @Test
    fun `should get certificate summary`() {
        // Given
        val eroId = aValidRandomEroId()
        val sourceType = aValidSourceType()
        val sourceReference = aValidSourceReference()
        val certificate = buildCertificate()
        given(certificateFinderService.getCertificate(any(), any(), any())).willReturn(certificate)
        val certificateSummaryDto = buildCertificateSummaryDto()
        given(mapper.certificateToCertificatePrintRequestSummaryDto(any())).willReturn(certificateSummaryDto)

        // When
        val actual = certificateSummaryService.getCertificateSummary(eroId, sourceType, sourceReference)

        // Then
        verify(certificateFinderService).getCertificate(eroId, sourceType, sourceReference)
        verify(mapper).certificateToCertificatePrintRequestSummaryDto(certificate)
        assertThat(actual).isSameAs(certificateSummaryDto)
    }
}
