package uk.gov.dluhc.printapi.service.tempcert

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
internal class ExplainerPdfServiceTest {

    @Mock
    private lateinit var eroClient: ElectoralRegistrationOfficeManagementApiClient
    @Mock
    private lateinit var explainerPdfFactory: ExplainerPdfFactory
    @InjectMocks
    private lateinit var explainerPdfService: ExplainerPdfService

    @Test
    fun `should `() {
        // Given
        val gssCode = "E99999999"
        val eroDto = buildEroDto()
        given(eroClient.getEro(any())).willReturn(eroDto)
        val contents = Random.Default.nextBytes(10)
        given(explainerPdfFactory.createPdfContents(any(), any())).willReturn(contents)

        // When
        val actual = explainerPdfService.generateExplainerPdf(gssCode)

        // Then
        verify(eroClient).getEro(gssCode)
        verify(explainerPdfFactory).createPdfContents(eroDto, gssCode)
        assertThat(actual.filename).isEqualTo("temporary-certificate-explainer-document-E99999999.pdf")
        assertThat(actual.contents).isSameAs(contents)
    }
}
