package uk.gov.dluhc.printapi.service.temporarycertificate

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
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
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.exception.GenerateTemporaryCertificateValidationException
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateMapper
import uk.gov.dluhc.printapi.service.S3AccessService
import uk.gov.dluhc.printapi.service.pdf.PdfFactory
import uk.gov.dluhc.printapi.service.pdf.TemporaryCertificatePdfTemplateDetailsFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.aTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.buildTemplateDetails
import uk.gov.dluhc.printapi.validator.service.GenerateTemporaryCertificateValidator
import java.net.URI
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
internal class TemporaryCertificateServiceTest {

    @Mock
    private lateinit var validator: GenerateTemporaryCertificateValidator

    @Mock
    private lateinit var eroClient: ElectoralRegistrationOfficeManagementApiClient

    @Mock
    private lateinit var temporaryCertificateRepository: TemporaryCertificateRepository

    @Mock
    private lateinit var temporaryCertificateMapper: TemporaryCertificateMapper

    @Mock
    private lateinit var temporaryCertificatePdfTemplateDetailsFactory: TemporaryCertificatePdfTemplateDetailsFactory

    @Mock
    private lateinit var pdfFactory: PdfFactory

    @Mock
    private lateinit var s3AccessService: S3AccessService

    @InjectMocks
    private lateinit var temporaryCertificateService: TemporaryCertificateService

    @Test
    fun `should generate temporary certificate pdf`() {
        // Given
        val eroId = aValidRandomEroId()
        val request = buildGenerateTemporaryCertificateDto()
        val certificateNumber = "ZlxBCBxpjseZU5i3ccyL"
        val eroDetails = buildEroDto(eroId = eroId)
        given(eroClient.getEro(any())).willReturn(eroDetails)
        val templateFilename = aTemplateFilename()
        given(temporaryCertificatePdfTemplateDetailsFactory.getTemplateFilename(any())).willReturn(templateFilename)
        val temporaryCertificate = buildTemporaryCertificate(certificateNumber = certificateNumber)
        given(temporaryCertificateMapper.toTemporaryCertificate(any(), any(), any())).willReturn(temporaryCertificate)
        val templateDetails = buildTemplateDetails()
        given(temporaryCertificatePdfTemplateDetailsFactory.getTemplateDetails(any())).willReturn(templateDetails)
        val contents = Random.Default.nextBytes(10)
        given(pdfFactory.createPdfContents(any())).willReturn(contents)
        val presignedUrl = URI.create("https://localhost/test-url")
        given(s3AccessService.uploadTemporaryCertificate(any(), any(), any(), any())).willReturn(presignedUrl)

        // When
        val actual = temporaryCertificateService.generateTemporaryCertificate(eroId, request)

        // Then
        verify(validator).validate(request)
        verify(eroClient).getEro(request.gssCode)
        verify(temporaryCertificatePdfTemplateDetailsFactory).getTemplateFilename(request.gssCode)
        verify(temporaryCertificateMapper).toTemporaryCertificate(request, eroDetails, templateFilename)
        verify(temporaryCertificatePdfTemplateDetailsFactory).getTemplateDetails(temporaryCertificate)
        verify(pdfFactory).createPdfContents(templateDetails)
        verify(temporaryCertificateRepository).save(temporaryCertificate)
        verify(s3AccessService).uploadTemporaryCertificate(
            request.gssCode,
            request.sourceReference,
            "temporary-certificate-ZlxBCBxpjseZU5i3ccyL.pdf",
            contents
        )
        assertThat(actual).isEqualTo(presignedUrl)
    }

    @Test
    fun `should fail to generate temporary certificate pdf as no ERO found by GssCode`() {
        // Given
        val eroId = aValidRandomEroId()
        val request = buildGenerateTemporaryCertificateDto(gssCode = "N06000012")
        given(eroClient.getEro(any())).willThrow(ElectoralRegistrationOfficeNotFoundException::class.java)

        // When
        val exception = catchThrowableOfType(GenerateTemporaryCertificateValidationException::class.java) {
            temporaryCertificateService.generateTemporaryCertificate(eroId, request)
        }

        // Then
        verify(validator).validate(request)
        verify(eroClient).getEro(request.gssCode)
        verifyNoInteractions(
            temporaryCertificatePdfTemplateDetailsFactory,
            temporaryCertificateMapper,
            temporaryCertificatePdfTemplateDetailsFactory,
            pdfFactory,
            temporaryCertificateRepository
        )
        assertThat(exception).hasMessage("Temporary Certificate gssCode 'N06000012' does not exist")
    }

    @Test
    fun `should fail to generate temporary certificate pdf as ERO found by GssCode does not belong to ERO making request`() {
        // Given
        val eroIdInRequest = "bath-and-north-east-somerset-council"
        val eroIdFoundByGssCode = "blackburn-with-darwen"
        val request = buildGenerateTemporaryCertificateDto(gssCode = "W06000023")
        val eroDetails = buildEroDto(eroId = eroIdFoundByGssCode)
        given(eroClient.getEro(any())).willReturn(eroDetails)

        // When
        val exception = catchThrowableOfType(GenerateTemporaryCertificateValidationException::class.java) {
            temporaryCertificateService.generateTemporaryCertificate(eroIdInRequest, request)
        }

        // Then
        verify(validator).validate(request)
        verify(eroClient).getEro(request.gssCode)
        verifyNoInteractions(
            temporaryCertificatePdfTemplateDetailsFactory,
            temporaryCertificateMapper,
            temporaryCertificatePdfTemplateDetailsFactory,
            pdfFactory,
            temporaryCertificateRepository
        )
        assertThat(exception)
            .hasMessage("Temporary Certificate gssCode 'W06000023' is not valid for eroId 'bath-and-north-east-somerset-council'")
    }
}
