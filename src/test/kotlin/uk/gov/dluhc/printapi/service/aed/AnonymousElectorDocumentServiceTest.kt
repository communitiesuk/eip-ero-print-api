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
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.exception.GenerateAnonymousElectorDocumentValidationException
import uk.gov.dluhc.printapi.mapper.AnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.AnonymousElectorSummaryMapper
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.service.pdf.PdfFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildAnonymousElectorDocumentSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.aTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.buildTemplateDetails
import java.time.Instant
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
internal class AnonymousElectorDocumentServiceTest {
    @Mock
    private lateinit var eroService: EroService

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
            val templateFilename = aTemplateFilename()
            val templateDetails = buildTemplateDetails()
            val anonymousElectorDocument = buildAnonymousElectorDocument(certificateNumber = certificateNumber)
            val contents = Random.Default.nextBytes(10)

            given(eroService.isGssCodeValidForEro(any(), any())).willReturn(true)
            given(pdfTemplateDetailsFactory.getTemplateFilename(any())).willReturn(templateFilename)
            given(anonymousElectorDocumentMapper.toAnonymousElectorDocument(any(), any())).willReturn(anonymousElectorDocument)
            given(pdfTemplateDetailsFactory.getTemplateDetails(any())).willReturn(templateDetails)
            given(pdfFactory.createPdfContents(any())).willReturn(contents)

            // When
            val actual = anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, request)

            // Then
            verify(eroService).isGssCodeValidForEro(request.gssCode, eroId)
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
            given(eroService.isGssCodeValidForEro(any(), any())).willThrow(ElectoralRegistrationOfficeNotFoundException::class.java)

            // When
            val exception = Assertions.catchThrowableOfType(
                { anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, request) },
                GenerateAnonymousElectorDocumentValidationException::class.java
            )

            // Then
            verify(eroService).isGssCodeValidForEro(request.gssCode, eroId)
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
            val request = buildGenerateAnonymousElectorDocumentDto(gssCode = "W06000023")
            given(eroService.isGssCodeValidForEro(any(), any())).willReturn(false)

            // When
            val exception = Assertions.catchThrowableOfType(
                { anonymousElectorDocumentService.generateAnonymousElectorDocument(eroIdInRequest, request) },
                GenerateAnonymousElectorDocumentValidationException::class.java
            )

            // Then
            verify(eroService).isGssCodeValidForEro(request.gssCode, eroIdInRequest)
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

            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReference(anyList(), any(), any())).willReturn(emptyList())

            // When
            val actual = anonymousElectorDocumentService.getAnonymousElectorDocumentSummaries(eroId, applicationId)

            // Then
            assertThat(actual).isNotNull.isEmpty()
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, applicationId)
            verifyNoInteractions(anonymousElectorSummaryMapper)
            verifyNoMoreInteractions(eroService, anonymousElectorDocumentRepository)
        }

        @Test
        fun `should return summary list in descending order of dateCreated for matching Anonymous Elector Documents`() {
            // Given
            val eroId = aValidRandomEroId()
            val applicationId = aValidSourceReference()
            val gssCodes = listOf(aGssCode())
            val firstAedEntity =
                buildAnonymousElectorDocument(
                    sourceReference = applicationId,
                    delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED)
                ).also { it.dateCreated = Instant.now() }

            val secondAedEntity =
                buildAnonymousElectorDocument(
                    sourceReference = applicationId,
                    delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED)
                ).also { it.dateCreated = Instant.now().plusSeconds(1) }

            val aedEntityWithLatestDateCreated =
                buildAnonymousElectorDocument(
                    sourceReference = applicationId,
                    delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.ERO_COLLECTION)
                ).also { it.dateCreated = Instant.now().plusSeconds(2) }

            val expectedDto1 = buildAnonymousElectorDocumentSummaryDto()
            val expectedDto2 = buildAnonymousElectorDocumentSummaryDto()
            val expectedDto3 = buildAnonymousElectorDocumentSummaryDto()

            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReference(anyList(), any(), any()))
                .willReturn(listOf(firstAedEntity, secondAedEntity, aedEntityWithLatestDateCreated))
            given(anonymousElectorSummaryMapper.mapToAnonymousElectorDocumentSummaryDto(firstAedEntity)).willReturn(expectedDto1)
            given(anonymousElectorSummaryMapper.mapToAnonymousElectorDocumentSummaryDto(secondAedEntity)).willReturn(expectedDto2)
            given(anonymousElectorSummaryMapper.mapToAnonymousElectorDocumentSummaryDto(aedEntityWithLatestDateCreated)).willReturn(expectedDto3)

            // When
            val actual = anonymousElectorDocumentService.getAnonymousElectorDocumentSummaries(eroId, applicationId)

            // Then
            assertThat(actual).isNotNull.hasSize(3)
                .usingRecursiveComparison()
                .isEqualTo(listOf(expectedDto3, expectedDto2, expectedDto1))
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, applicationId)
            verify(anonymousElectorSummaryMapper).mapToAnonymousElectorDocumentSummaryDto(firstAedEntity)
            verify(anonymousElectorSummaryMapper).mapToAnonymousElectorDocumentSummaryDto(secondAedEntity)
            verify(anonymousElectorSummaryMapper).mapToAnonymousElectorDocumentSummaryDto(aedEntityWithLatestDateCreated)
            verifyNoMoreInteractions(eroService, anonymousElectorDocumentRepository, anonymousElectorSummaryMapper)
        }
    }
}
