package uk.gov.dluhc.printapi.service.pdf

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
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
import uk.gov.dluhc.printapi.exception.ExplainerDocumentNotFoundException
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.buildTemplateDetails
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
internal class ExplainerPdfServiceTest {

    @Mock
    private lateinit var eroClient: ElectoralRegistrationOfficeManagementApiClient

    @Mock
    private lateinit var explainerPdfTemplateDetailsFactory: ExplainerPdfTemplateDetailsFactory

    @Mock
    private lateinit var pdfFactory: PdfFactory

    @InjectMocks
    private lateinit var explainerPdfService: ExplainerPdfService

    @Test
    fun `should generate explainer pdf`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCode = "E99999999"
        val eroDto = buildEroDto(eroId = eroId)
        given(eroClient.getEro(any())).willReturn(eroDto)
        val templateDetails = buildTemplateDetails()
        given(explainerPdfTemplateDetailsFactory.getTemplateDetails(any(), any())).willReturn(templateDetails)
        given(explainerPdfTemplateDetailsFactory.getDownloadFilenamePrefix()).willReturn("temporary-certificate-explainer-document")
        val contents = Random.Default.nextBytes(10)
        given(pdfFactory.createPdfContents(any())).willReturn(contents)

        // When
        val actual = explainerPdfService.generateExplainerPdf(eroId, gssCode)

        // Then
        verify(eroClient).getEro(gssCode)
        verify(explainerPdfTemplateDetailsFactory).getTemplateDetails(gssCode, eroDto)
        verify(pdfFactory).createPdfContents(templateDetails)
        assertThat(actual.filename).isEqualTo("temporary-certificate-explainer-document-E99999999.pdf")
        assertThat(actual.contents).isSameAs(contents)
    }

    @Test
    fun `should raise not found exception given ERO in context is different to one found by GSS Code`() {
        // Given
        val eroId = "greenwich-london-borough-council"
        val gssCode = aGssCode()
        val eroDto = buildEroDto(eroId = "newport-city-council")
        given(eroClient.getEro(any())).willReturn(eroDto)
        given(explainerPdfTemplateDetailsFactory.getExceptionMessage(any(), any()))
            .willReturn("Temporary certificate explainer document not found for eroId $eroId and gssCode $gssCode")
        val expected = ExplainerDocumentNotFoundException("Temporary certificate explainer document not found for eroId $eroId and gssCode $gssCode")

        // When
        val error = catchException { explainerPdfService.generateExplainerPdf(eroId, gssCode) }

        // Then
        verify(eroClient).getEro(gssCode)
        verify(explainerPdfTemplateDetailsFactory).getExceptionMessage(eroId, gssCode)
        verifyNoInteractions(pdfFactory)
        assertThat(error)
            .isInstanceOf(ExplainerDocumentNotFoundException::class.java)
            .hasMessage(expected.message)
    }

    @Test
    fun `should raise not found exception given ERO raised not found`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCode = aGssCode()
        given(eroClient.getEro(any())).willThrow(ElectoralRegistrationOfficeNotFoundException::class.java)
        given(explainerPdfTemplateDetailsFactory.getExceptionMessage(any(), any()))
            .willReturn("Temporary certificate explainer document not found for eroId $eroId and gssCode $gssCode")
        val expected = ExplainerDocumentNotFoundException("Temporary certificate explainer document not found for eroId $eroId and gssCode $gssCode")

        // When
        val error = catchException { explainerPdfService.generateExplainerPdf(eroId, gssCode) }

        // Then
        verify(eroClient).getEro(gssCode)
        verify(explainerPdfTemplateDetailsFactory).getExceptionMessage(eroId, gssCode)
        verifyNoInteractions(pdfFactory)
        assertThat(error)
            .isInstanceOf(ExplainerDocumentNotFoundException::class.java)
            .hasMessage(expected.message)
    }
}
