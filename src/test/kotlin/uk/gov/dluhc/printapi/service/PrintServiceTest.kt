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
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.certificateBuilder
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
        val ero = buildEroManagementApiEroDto()
        val localAuthority = ero.localAuthorities[1]
        val message = buildSendApplicationToPrintMessage(gssCode = localAuthority.gssCode)
        val certificate = certificateBuilder()
        given(eroClient.getElectoralRegistrationOffice(any())).willReturn(ero)
        given(certificateMapper.toCertificate(any(), any(), any())).willReturn(certificate)

        // When
        printService.savePrintMessage(message)

        // Then
        verify(eroClient).getElectoralRegistrationOffice(message.gssCode!!)
        verify(certificateRepository).save(certificate)
    }
}
