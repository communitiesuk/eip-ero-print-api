package uk.gov.dluhc.printapi.service.temporarycertificate

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateMapper
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.aTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.buildTemplateDetails
import uk.gov.dluhc.printapi.validator.service.GenerateTemporaryCertificateValidator
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
    private lateinit var certificatePdfTemplateDetailsFactory: CertificatePdfTemplateDetailsFactory

    @Mock
    private lateinit var pdfFactory: PdfFactory

    @InjectMocks
    private lateinit var temporaryCertificateService: TemporaryCertificateService

    @Test
    fun `should generate temporary certificate pdf`() {
        // Given
        val request = buildGenerateTemporaryCertificateDto()
        val certificateNumber = "ZlxBCBxpjseZU5i3ccyL"
        val eroDetails = buildEroDto()
        given(eroClient.getEro(any())).willReturn(eroDetails)
        val templateFilename = aTemplateFilename()
        given(certificatePdfTemplateDetailsFactory.getTemplateFilename(any())).willReturn(templateFilename)
        val temporaryCertificate = buildTemporaryCertificate(certificateNumber = certificateNumber)
        given(temporaryCertificateMapper.toTemporaryCertificate(any(), any(), any())).willReturn(temporaryCertificate)
        val templateDetails = buildTemplateDetails()
        given(certificatePdfTemplateDetailsFactory.getTemplateDetails(any())).willReturn(templateDetails)
        val contents = Random.Default.nextBytes(10)
        given(pdfFactory.createPdfContents(any())).willReturn(contents)

        // When
        val actual = temporaryCertificateService.generateTemporaryCertificate(request)

        // Then
        verify(validator).validate(request)
        verify(eroClient).getEro(request.gssCode)
        verify(certificatePdfTemplateDetailsFactory).getTemplateFilename(request.gssCode)
        verify(temporaryCertificateMapper).toTemporaryCertificate(request, eroDetails, templateFilename)
        verify(certificatePdfTemplateDetailsFactory).getTemplateDetails(temporaryCertificate)
        verify(temporaryCertificateRepository).save(temporaryCertificate)
        verify(pdfFactory).createPdfContents(templateDetails)
        Assertions.assertThat(actual.filename).isEqualTo("temporary-certificate-ZlxBCBxpjseZU5i3ccyL.pdf")
        Assertions.assertThat(actual.contents).isSameAs(contents)
    }
}
