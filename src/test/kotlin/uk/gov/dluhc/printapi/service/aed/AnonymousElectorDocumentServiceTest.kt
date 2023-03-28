package uk.gov.dluhc.printapi.service.aed

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.exception.GenerateAnonymousElectorDocumentValidationException
import uk.gov.dluhc.printapi.mapper.AnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.AnonymousElectorSummaryMapper
import uk.gov.dluhc.printapi.service.pdf.PdfFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildAnonymousElectorDocumentSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.aTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.buildTemplateDetails
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
internal class AnonymousElectorDocumentServiceTest {
    @Mock
    private lateinit var eroClient: ElectoralRegistrationOfficeManagementApiClient

    @Mock
    private lateinit var anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository

    @Mock
    private lateinit var anonymousElectorDocumentMapper: AnonymousElectorDocumentMapper

    @Mock
    private lateinit var anonymousElectorSummaryMapper: AnonymousElectorSummaryMapper

    @Mock
    private lateinit var pdfTemplateDetailsFactory: AedPdfTemplateDetailsFactory

    @Mock
    private lateinit var pdfFactory: PdfFactory

    @InjectMocks
    private lateinit var anonymousElectorDocumentService: AnonymousElectorDocumentService

    @Nested
    inner class GenerateAnonymousElectorDocument {
        @Test
        fun `should generate Anonymous Elector Document pdf`() {
            // Given
            val eroId = aValidRandomEroId()
            val request = buildGenerateAnonymousElectorDocumentDto()
            val certificateNumber = "ZlxBCBxpjseZU5i3ccyL"
            val eroDetails = buildEroDto(eroId = eroId)
            val templateFilename = aTemplateFilename()
            val templateDetails = buildTemplateDetails()
            val anonymousElectorDocument = buildAnonymousElectorDocument(certificateNumber = certificateNumber)
            val contents = Random.Default.nextBytes(10)

            given(eroClient.getEro(any())).willReturn(eroDetails)
            given(pdfTemplateDetailsFactory.getTemplateFilename(any())).willReturn(templateFilename)
            given(anonymousElectorDocumentMapper.toAnonymousElectorDocument(any(), any())).willReturn(anonymousElectorDocument)
            given(pdfTemplateDetailsFactory.getTemplateDetails(any())).willReturn(templateDetails)
            given(pdfFactory.createPdfContents(any())).willReturn(contents)

            // When
            val actual = anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, request)

            // Then
            verify(eroClient).getEro(request.gssCode)
            verify(pdfTemplateDetailsFactory).getTemplateFilename(request.gssCode)
            verify(anonymousElectorDocumentMapper).toAnonymousElectorDocument(request, templateFilename)
            verify(pdfTemplateDetailsFactory).getTemplateDetails(anonymousElectorDocument)
            verify(pdfFactory).createPdfContents(templateDetails)
            verify(anonymousElectorDocumentRepository).save(anonymousElectorDocument)
            assertThat(actual.filename).isEqualTo("anonymous-elector-document-ZlxBCBxpjseZU5i3ccyL.pdf")
            assertThat(actual.contents).isSameAs(contents)
        }

        @Test
        fun `should fail to generate Anonymous Elector Document pdf as no ERO found by GssCode`() {
            // Given
            val eroId = aValidRandomEroId()
            val request = buildGenerateAnonymousElectorDocumentDto(gssCode = "N06000012")
            given(eroClient.getEro(any())).willThrow(ElectoralRegistrationOfficeNotFoundException::class.java)

            // When
            val exception = Assertions.catchThrowableOfType(
                { anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, request) },
                GenerateAnonymousElectorDocumentValidationException::class.java
            )

            // Then
            verify(eroClient).getEro(request.gssCode)
            verifyNoInteractions(
                pdfTemplateDetailsFactory,
                anonymousElectorDocumentMapper,
                pdfTemplateDetailsFactory,
                pdfFactory,
                anonymousElectorDocumentRepository
            )
            assertThat(exception).hasMessage("Anonymous Elector Document gssCode 'N06000012' does not exist")
        }

        @Test
        fun `should fail to generate Anonymous Elector Document pdf as ERO found by GssCode does not belong to ERO making request`() {
            // Given
            val eroIdInRequest = "bath-and-north-east-somerset-council"
            val eroIdFoundByGssCode = "blackburn-with-darwen"
            val request = buildGenerateAnonymousElectorDocumentDto(gssCode = "W06000023")
            val eroDetails = buildEroDto(eroId = eroIdFoundByGssCode)
            given(eroClient.getEro(any())).willReturn(eroDetails)

            // When
            val exception = Assertions.catchThrowableOfType(
                { anonymousElectorDocumentService.generateAnonymousElectorDocument(eroIdInRequest, request) },
                GenerateAnonymousElectorDocumentValidationException::class.java
            )

            // Then
            verify(eroClient).getEro(request.gssCode)
            verifyNoInteractions(
                pdfTemplateDetailsFactory,
                anonymousElectorDocumentMapper,
                pdfTemplateDetailsFactory,
                pdfFactory,
                anonymousElectorDocumentRepository
            )
            assertThat(exception)
                .hasMessage("Anonymous Elector Document gssCode 'W06000023' is not valid for eroId 'bath-and-north-east-somerset-council'")
        }
    }

    @Nested
    inner class GetAnonymousElectorDocumentSummaries {
        @Test
        fun `should return empty summaries for a non existing Anonymous Elector Document pdf`() {
            // Given
            val eroId = aValidRandomEroId()
            val applicationId = aValidSourceReference()
            val gssCodes = listOf(aGssCode(), aGssCode())

            given(eroClient.getElectoralRegistrationOfficeGssCodes(any())).willReturn(gssCodes)
            given(anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReference(anyList(), any(), any())).willReturn(emptyList())

            // When
            val actual = anonymousElectorDocumentService.getAnonymousElectorDocumentSummaries(eroId, applicationId)

            // Then
            assertThat(actual).isNotNull.isEmpty()
            verify(eroClient).getElectoralRegistrationOfficeGssCodes(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, applicationId)
            verifyNoInteractions(anonymousElectorSummaryMapper)
            verifyNoMoreInteractions(eroClient, anonymousElectorDocumentRepository)
        }

        @Test
        fun `should return summary list for a matching Anonymous Elector Document pdf`() {
            // Given
            val eroId = aValidRandomEroId()
            val applicationId = aValidSourceReference()
            val gssCodes = listOf(aGssCode())
            val aedEntity1 = buildAnonymousElectorDocument(delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED))
            val aedEntity2 = buildAnonymousElectorDocument(delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.ERO_COLLECTION))
            val expectedDto1 = buildAnonymousElectorDocumentSummaryDto()
            val expectedDto2 = buildAnonymousElectorDocumentSummaryDto()

            given(eroClient.getElectoralRegistrationOfficeGssCodes(any())).willReturn(gssCodes)
            given(anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReference(anyList(), any(), any())).willReturn(listOf(aedEntity1, aedEntity2))
            given(anonymousElectorSummaryMapper.mapToAnonymousElectorDocumentSummaryDto(aedEntity1)).willReturn(expectedDto1)
            given(anonymousElectorSummaryMapper.mapToAnonymousElectorDocumentSummaryDto(aedEntity2)).willReturn(expectedDto2)

            // When
            val actual = anonymousElectorDocumentService.getAnonymousElectorDocumentSummaries(eroId, applicationId)

            // Then
            assertThat(actual).isNotNull.hasSize(2)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(listOf(expectedDto1, expectedDto2))
            verify(eroClient).getElectoralRegistrationOfficeGssCodes(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, applicationId)
            verify(anonymousElectorSummaryMapper).mapToAnonymousElectorDocumentSummaryDto(aedEntity1)
            verify(anonymousElectorSummaryMapper).mapToAnonymousElectorDocumentSummaryDto(aedEntity2)
            verifyNoMoreInteractions(eroClient, anonymousElectorDocumentRepository, anonymousElectorSummaryMapper)
        }
    }
}
