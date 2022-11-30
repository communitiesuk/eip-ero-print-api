package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.mapper.CertificateMapper
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage

@ExtendWith(MockitoExtension::class)
class PrintServiceTest {
    @InjectMocks
    private lateinit var printService: PrintService

    @Mock
    private lateinit var eroClient: ElectoralRegistrationOfficeManagementApiClient

    @Mock
    private lateinit var certificateMapper: CertificateMapper

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Test
    fun `should save send application to certificate`() {
        // Given
        val issuer = buildEroDto()
        val message = buildSendApplicationToPrintMessage()
        val certificate = buildCertificate()
        given(eroClient.getIssuer(any())).willReturn(issuer)
        given(certificateMapper.toCertificate(any(), any())).willReturn(certificate)

        // When
        printService.savePrintMessage(message)

        // Then
        verify(eroClient).getIssuer(message.gssCode!!)
        verify(certificateMapper).toCertificate(message, issuer)
        verify(certificateRepository).save(certificate)
    }
}
