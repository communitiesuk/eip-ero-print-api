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
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.mapper.CertificateMapper
import uk.gov.dluhc.printapi.mapper.PrintRequestMapper
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage

@ExtendWith(MockitoExtension::class)
class PrintServiceTest {
    @InjectMocks
    private lateinit var printService: PrintService

    @Mock
    private lateinit var eroClient: ElectoralRegistrationOfficeManagementApiClient

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var certificateMapper: CertificateMapper

    @Mock
    private lateinit var printRequestMapper: PrintRequestMapper

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Test
    fun `should save send application to certificate given first application print request`() {
        // Given
        val ero = buildEroDto()
        val message = buildSendApplicationToPrintMessage()
        val sourceType = aValidSourceType()
        val certificate = buildCertificate()
        given(eroClient.getEro(any())).willReturn(ero)
        given(
            certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(
                any(),
                any(),
                any()
            )
        ).willReturn(null)
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(sourceType)
        given(certificateMapper.toCertificate(any(), any())).willReturn(certificate)

        // When
        printService.savePrintMessage(message)

        // Then
        verify(eroClient).getEro(message.gssCode!!)
        verify(certificateRepository).findByGssCodeInAndSourceTypeAndSourceReference(
            listOf(message.gssCode!!),
            sourceType,
            message.sourceReference
        )
        verify(sourceTypeMapper).toSourceTypeEntity(message.sourceType)
        verify(certificateMapper).toCertificate(message, ero)
        verify(certificateRepository).save(certificate)
        verifyNoInteractions(printRequestMapper)
    }

    @Test
    fun `should save send application to certificate given re-print request`() {
        // Given
        val ero = buildEroDto()
        val message = buildSendApplicationToPrintMessage()
        val sourceType = aValidSourceType()
        val printRequest = buildPrintRequest()
        val certificate = buildCertificate(printRequests = listOf(printRequest))
        val newPrintRequest = buildPrintRequest()
        given(eroClient.getEro(any())).willReturn(ero)
        given(certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(
            certificate
        )
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(sourceType)
        given(printRequestMapper.toPrintRequest(any(), any())).willReturn(newPrintRequest)

        // When
        printService.savePrintMessage(message)

        // Then
        verify(eroClient).getEro(message.gssCode!!)
        verify(certificateRepository).findByGssCodeInAndSourceTypeAndSourceReference(
            listOf(message.gssCode!!),
            sourceType,
            message.sourceReference
        )
        verify(sourceTypeMapper).toSourceTypeEntity(message.sourceType)
        verify(printRequestMapper).toPrintRequest(message, ero)
        verify(certificateRepository).save(certificate)
        assertThat(certificate.printRequests).usingRecursiveComparison()
            .isEqualTo(listOf(printRequest, newPrintRequest))
    }
}
