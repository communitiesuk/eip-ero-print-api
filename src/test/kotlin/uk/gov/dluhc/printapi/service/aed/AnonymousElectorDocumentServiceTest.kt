package uk.gov.dluhc.printapi.service.aed

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
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
import uk.gov.dluhc.printapi.exception.CertificateNotFoundException
import uk.gov.dluhc.printapi.exception.GenerateAnonymousElectorDocumentValidationException
import uk.gov.dluhc.printapi.exception.UpdateAnonymousElectorDocumentAllInitialDataRemovedException
import uk.gov.dluhc.printapi.mapper.aed.AnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.GenerateAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.ReIssueAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.service.S3AccessService
import uk.gov.dluhc.printapi.service.pdf.PdfFactory
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildGenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildUpdateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.aTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.buildTemplateDetails
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.random.Random

@ExtendWith(MockitoExtension::class)
internal class AnonymousElectorDocumentServiceTest {
    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository

    @Mock
    private lateinit var generateAnonymousElectorDocumentMapper: GenerateAnonymousElectorDocumentMapper

    @Mock
    private lateinit var reIssueAnonymousElectorDocumentMapper: ReIssueAnonymousElectorDocumentMapper

    @Mock
    private lateinit var anonymousElectorDocumentMapper: AnonymousElectorDocumentMapper

    @Mock
    private lateinit var pdfTemplateDetailsFactory: AedPdfTemplateDetailsFactory

    @Mock
    private lateinit var pdfFactory: PdfFactory

    @Mock
    private lateinit var s3AccessService: S3AccessService

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
            val presignedUrl = URI.create("https://localhost/test-url")

            given(eroService.isGssCodeValidForEro(any(), any())).willReturn(true)
            given(pdfTemplateDetailsFactory.getTemplateFilename(any())).willReturn(templateFilename)
            given(generateAnonymousElectorDocumentMapper.toAnonymousElectorDocument(any(), any())).willReturn(
                anonymousElectorDocument
            )
            given(pdfTemplateDetailsFactory.getTemplateDetails(any())).willReturn(templateDetails)
            given(pdfFactory.createPdfContents(any())).willReturn(contents)
            given(s3AccessService.uploadAed(any(), any(), any(), any())).willReturn(presignedUrl)

            // When
            val actual = anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, request)

            // Then
            verify(eroService).isGssCodeValidForEro(request.gssCode, eroId)
            verify(pdfTemplateDetailsFactory).getTemplateFilename(request.gssCode)
            verify(generateAnonymousElectorDocumentMapper).toAnonymousElectorDocument(request, templateFilename)
            verify(pdfTemplateDetailsFactory).getTemplateDetails(anonymousElectorDocument)
            verify(pdfFactory).createPdfContents(templateDetails)
            verify(anonymousElectorDocumentRepository).save(anonymousElectorDocument)
            verify(s3AccessService).uploadAed(
                anonymousElectorDocument.gssCode,
                anonymousElectorDocument.sourceReference,
                "anonymous-elector-document-ZlxBCBxpjseZU5i3ccyL.pdf",
                contents
            )
            assertThat(actual).isEqualTo(presignedUrl)
        }

        @Test
        fun `should fail to generate Anonymous Elector Document pdf as no ERO found by GssCode`() {
            // Given
            val eroId = aValidRandomEroId()
            val request = buildGenerateAnonymousElectorDocumentDto(gssCode = "N06000012")
            given(
                eroService.isGssCodeValidForEro(
                    any(),
                    any()
                )
            ).willThrow(ElectoralRegistrationOfficeNotFoundException::class.java)

            // When
            val exception = catchThrowableOfType(GenerateAnonymousElectorDocumentValidationException::class.java) {
                anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, request)
            }

            // Then
            verify(eroService).isGssCodeValidForEro(request.gssCode, eroId)
            verifyNoInteractions(
                pdfTemplateDetailsFactory,
                generateAnonymousElectorDocumentMapper,
                pdfTemplateDetailsFactory,
                pdfFactory,
                s3AccessService,
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
            val exception = catchThrowableOfType(GenerateAnonymousElectorDocumentValidationException::class.java) {
                anonymousElectorDocumentService.generateAnonymousElectorDocument(eroIdInRequest, request)
            }

            // Then
            verify(eroService).isGssCodeValidForEro(request.gssCode, eroIdInRequest)
            verifyNoInteractions(
                pdfTemplateDetailsFactory,
                generateAnonymousElectorDocumentMapper,
                pdfTemplateDetailsFactory,
                pdfFactory,
                s3AccessService,
                anonymousElectorDocumentRepository
            )
            assertThat(exception)
                .hasMessage("Anonymous Elector Document gssCode 'W06000023' is not valid for eroId 'bath-and-north-east-somerset-council'")
        }
    }

    @Nested
    inner class GetAnonymousElectorDocument {
        @Test
        fun `should return empty list for a non existing Anonymous Elector Document pdf`() {
            // Given
            val eroId = aValidRandomEroId()
            val applicationId = aValidSourceReference()
            val gssCodes = listOf(aGssCode(), aGssCode())

            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    anyList(),
                    any(),
                    any(),
                )
            ).willReturn(emptyList())

            // When
            val actual = anonymousElectorDocumentService.getAnonymousElectorDocuments(eroId, applicationId)

            // Then
            assertThat(actual).isNotNull.isEmpty()
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                gssCodes,
                ANONYMOUS_ELECTOR_DOCUMENT,
                applicationId,
            )
            verifyNoInteractions(anonymousElectorDocumentMapper)
            verifyNoMoreInteractions(eroService, anonymousElectorDocumentRepository)
        }

        @Test
        fun `should return list in descending order of dateCreated for matching Anonymous Elector Documents`() {
            // Given
            val eroId = aValidRandomEroId()
            val applicationId = aValidSourceReference()
            val gssCodes = listOf(aGssCode())
            val firstAedEntity =
                buildAnonymousElectorDocument(
                    sourceReference = applicationId,
                    delivery = buildAedDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED)
                ).also { it.dateCreated = Instant.now() }

            val secondAedEntity =
                buildAnonymousElectorDocument(
                    sourceReference = applicationId,
                    delivery = buildAedDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED)
                ).also { it.dateCreated = Instant.now().plusSeconds(1) }

            val aedEntityWithLatestDateCreated =
                buildAnonymousElectorDocument(
                    sourceReference = applicationId,
                    delivery = buildAedDelivery(deliveryAddressType = DeliveryAddressType.ERO_COLLECTION)
                ).also { it.dateCreated = Instant.now().plusSeconds(2) }

            val expectedDto1 = buildAnonymousElectorDocumentDto()
            val expectedDto2 = buildAnonymousElectorDocumentDto()
            val expectedDto3 = buildAnonymousElectorDocumentDto()

            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    anyList(),
                    any(),
                    any(),
                )
            )
                .willReturn(listOf(aedEntityWithLatestDateCreated, secondAedEntity, firstAedEntity))
            given(anonymousElectorDocumentMapper.mapToAnonymousElectorDocumentDto(firstAedEntity)).willReturn(
                expectedDto1
            )
            given(anonymousElectorDocumentMapper.mapToAnonymousElectorDocumentDto(secondAedEntity)).willReturn(
                expectedDto2
            )
            given(anonymousElectorDocumentMapper.mapToAnonymousElectorDocumentDto(aedEntityWithLatestDateCreated)).willReturn(
                expectedDto3
            )

            // When
            val actual = anonymousElectorDocumentService.getAnonymousElectorDocuments(eroId, applicationId)

            // Then
            assertThat(actual).isNotNull.hasSize(3)
                .usingRecursiveComparison()
                .isEqualTo(listOf(expectedDto3, expectedDto2, expectedDto1))
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                gssCodes,
                ANONYMOUS_ELECTOR_DOCUMENT,
                applicationId,
            )
            verify(anonymousElectorDocumentMapper).mapToAnonymousElectorDocumentDto(firstAedEntity)
            verify(anonymousElectorDocumentMapper).mapToAnonymousElectorDocumentDto(secondAedEntity)
            verify(anonymousElectorDocumentMapper).mapToAnonymousElectorDocumentDto(aedEntityWithLatestDateCreated)
            verifyNoMoreInteractions(eroService, anonymousElectorDocumentRepository, anonymousElectorDocumentMapper)
        }
    }

    @Nested
    inner class ReIssueAnonymousElectorDocument {

        @Test
        fun `should re-issue AED given previous AEDs exist for application ID`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())
            val sourceReference = aValidSourceReference()

            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)

            val firstIssuedAed = buildAnonymousElectorDocument(
                gssCode = gssCodes.first(),
                sourceReference = sourceReference
            ).apply {
                dateCreated = Instant.now().minus(10, ChronoUnit.DAYS)
            }
            val mostRecentIssueAed = buildAnonymousElectorDocument(
                gssCode = gssCodes.first(),
                sourceReference = sourceReference
            ).apply {
                dateCreated = Instant.now().minus(2, ChronoUnit.DAYS)
            }
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            )
                .willReturn(listOf(mostRecentIssueAed, firstIssuedAed))

            val templateFilename = aTemplateFilename()
            given(pdfTemplateDetailsFactory.getTemplateFilename(any())).willReturn(templateFilename)
            val templateDetails = buildTemplateDetails()
            given(pdfTemplateDetailsFactory.getTemplateDetails(any())).willReturn(templateDetails)
            val contents = Random.Default.nextBytes(10)
            given(pdfFactory.createPdfContents(any())).willReturn(contents)
            val presignedUrl = URI.create("https://localhost/test-url")
            given(s3AccessService.uploadAed(any(), any(), any(), any())).willReturn(presignedUrl)

            val certificateNumber = "ZlxBCBxpjseZU5i3ccyL"
            val newlyIssuedAed = buildAnonymousElectorDocument(
                certificateNumber = certificateNumber
            )
            given(reIssueAnonymousElectorDocumentMapper.toNewAnonymousElectorDocument(any(), any(), any()))
                .willReturn(newlyIssuedAed)

            val dto = buildReIssueAnonymousElectorDocumentDto(
                sourceReference = sourceReference
            )

            // When
            val actual = anonymousElectorDocumentService.reIssueAnonymousElectorDocument(eroId, dto)

            // Then
            assertThat(actual).isEqualTo(presignedUrl)
            verify(s3AccessService).uploadAed(
                newlyIssuedAed.gssCode,
                newlyIssuedAed.sourceReference,
                "anonymous-elector-document-ZlxBCBxpjseZU5i3ccyL.pdf",
                contents
            )
            verify(pdfTemplateDetailsFactory).getTemplateFilename(gssCodes.first())
            verify(pdfTemplateDetailsFactory).getTemplateDetails(newlyIssuedAed)
            verify(pdfFactory).createPdfContents(templateDetails)
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                gssCodes,
                ANONYMOUS_ELECTOR_DOCUMENT,
                dto.sourceReference,
            )
            verify(reIssueAnonymousElectorDocumentMapper).toNewAnonymousElectorDocument(
                mostRecentIssueAed,
                dto,
                templateFilename
            )
            verify(anonymousElectorDocumentRepository).save(newlyIssuedAed)
        }

        @Test
        fun `should re-issue AED given previous AED has passed initial data retention period`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())
            val sourceReference = aValidSourceReference()
            val mostRecentIssueAed =
                buildAnonymousElectorDocument(gssCode = gssCodes.first(), sourceReference = sourceReference)
                    .also { it.removeInitialRetentionPeriodData() }
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(any(), any(), any())
            ).willReturn(listOf(mostRecentIssueAed))

            val templateFilename = aTemplateFilename()
            given(pdfTemplateDetailsFactory.getTemplateFilename(any())).willReturn(templateFilename)
            val templateDetails = buildTemplateDetails()
            given(pdfTemplateDetailsFactory.getTemplateDetails(any())).willReturn(templateDetails)
            val contents = Random.Default.nextBytes(10)
            given(pdfFactory.createPdfContents(any())).willReturn(contents)
            val presignedUrl = URI.create("https://localhost/test-url")
            given(s3AccessService.uploadAed(any(), any(), any(), any())).willReturn(presignedUrl)

            val certificateNumber = "ZlxBCBxpjseZU5i3ccyL"
            val newlyIssuedAed = buildAnonymousElectorDocument(
                certificateNumber = certificateNumber
            )
            given(reIssueAnonymousElectorDocumentMapper.toNewAnonymousElectorDocument(any(), any(), any()))
                .willReturn(newlyIssuedAed)

            val dto = buildReIssueAnonymousElectorDocumentDto(
                sourceReference = sourceReference
            )

            // When
            val actual = anonymousElectorDocumentService.reIssueAnonymousElectorDocument(eroId, dto)

            // Then
            assertThat(actual).isEqualTo(presignedUrl)
            verify(s3AccessService).uploadAed(
                newlyIssuedAed.gssCode,
                newlyIssuedAed.sourceReference,
                "anonymous-elector-document-ZlxBCBxpjseZU5i3ccyL.pdf",
                contents
            )
            verify(pdfTemplateDetailsFactory).getTemplateFilename(gssCodes.first())
            verify(pdfTemplateDetailsFactory).getTemplateDetails(newlyIssuedAed)
            verify(pdfFactory).createPdfContents(templateDetails)
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                gssCodes,
                ANONYMOUS_ELECTOR_DOCUMENT,
                dto.sourceReference,
            )
            verify(reIssueAnonymousElectorDocumentMapper).toNewAnonymousElectorDocument(
                mostRecentIssueAed,
                dto,
                templateFilename
            )
            verify(anonymousElectorDocumentRepository).save(newlyIssuedAed)
        }

        @Test
        fun `should not re-issue AED given no previous AED exists for application ID`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())

            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)

            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            )
                .willReturn(emptyList())

            val dto = buildReIssueAnonymousElectorDocumentDto()

            // When
            val exception = catchThrowableOfType(CertificateNotFoundException::class.java) {
                anonymousElectorDocumentService.reIssueAnonymousElectorDocument(eroId, dto)
            }

            // Then
            assertThat(exception)
                .hasMessage("Certificate for eroId = $eroId with sourceType = ANONYMOUS_ELECTOR_DOCUMENT and sourceReference = ${dto.sourceReference} not found")
            verify(eroService).lookupGssCodesForEro(eroId)
            verify(anonymousElectorDocumentRepository).findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                gssCodes,
                ANONYMOUS_ELECTOR_DOCUMENT,
                dto.sourceReference,
            )
        }
    }

    @Nested
    inner class UpdateAnonymousElectorDocuments {
        @Test
        fun `should update elector's email address on single AED`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())
            val originalEmailAddress = aValidEmailAddress()
            val originalPhoneNumber = aValidPhoneNumber()
            val aed = buildAnonymousElectorDocument(
                gssCode = gssCodes.first(),
                contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
            )
            val newEmailAddress = anotherValidEmailAddress()
            val updateAedDto = buildUpdateAnonymousElectorDocumentDto(
                sourceReference = aed.sourceReference,
                email = newEmailAddress,
                phoneNumber = null
            )
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            ).willReturn(listOf(aed))

            // When
            anonymousElectorDocumentService.updateAnonymousElectorDocuments(eroId, updateAedDto)

            // Then
            assertThat(aed.contactDetails!!.email).isEqualTo(newEmailAddress)
            assertThat(aed.contactDetails!!.phoneNumber).isEqualTo(originalPhoneNumber)
        }

        @Test
        fun `should update elector's phone number on single AED`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())
            val originalEmailAddress = aValidEmailAddress()
            val originalPhoneNumber = aValidPhoneNumber()
            val aed = buildAnonymousElectorDocument(
                gssCode = gssCodes.first(),
                contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
            )
            val newPhoneNumber = anotherValidPhoneNumber()
            val updateAedDto = buildUpdateAnonymousElectorDocumentDto(
                sourceReference = aed.sourceReference,
                email = null,
                phoneNumber = newPhoneNumber
            )
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            ).willReturn(listOf(aed))

            // When
            anonymousElectorDocumentService.updateAnonymousElectorDocuments(eroId, updateAedDto)

            // Then
            assertThat(aed.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
            assertThat(aed.contactDetails!!.email).isEqualTo(originalEmailAddress)
        }

        @Test
        fun `should update elector's email address and phone number on single AED`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())
            val originalEmailAddress = aValidEmailAddress()
            val originalPhoneNumber = aValidPhoneNumber()
            val aed = buildAnonymousElectorDocument(
                gssCode = gssCodes.first(),
                contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
            )
            val newEmailAddress = anotherValidEmailAddress()
            val newPhoneNumber = anotherValidPhoneNumber()
            val updateAedDto = buildUpdateAnonymousElectorDocumentDto(
                sourceReference = aed.sourceReference,
                email = newEmailAddress,
                phoneNumber = newPhoneNumber
            )
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            ).willReturn(listOf(aed))

            // When
            anonymousElectorDocumentService.updateAnonymousElectorDocuments(eroId, updateAedDto)

            // Then
            assertThat(aed.contactDetails!!.email).isEqualTo(newEmailAddress)
            assertThat(aed.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
        }

        @Test
        fun `should update elector's email address and phone number on multiple AEDs`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())
            val sourceReference = aValidSourceReference()
            val originalEmail = aValidEmailAddress()
            val originalPhoneNumber = aValidPhoneNumber()
            val aed1 = buildAnonymousElectorDocument(
                sourceReference = sourceReference,
                gssCode = gssCodes.first(),
                contactDetails = buildAedContactDetails(email = originalEmail, phoneNumber = originalPhoneNumber)
            )
            val aed2 = buildAnonymousElectorDocument(
                sourceReference = sourceReference,
                gssCode = gssCodes.first(),
                contactDetails = buildAedContactDetails(email = originalEmail, phoneNumber = originalPhoneNumber)
            )
            val newEmailAddress = anotherValidEmailAddress()
            val newPhoneNumber = anotherValidPhoneNumber()
            val updateAedDto = buildUpdateAnonymousElectorDocumentDto(
                sourceReference = sourceReference,
                email = newEmailAddress,
                phoneNumber = newPhoneNumber
            )
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            ).willReturn(listOf(aed1, aed2))

            // When
            anonymousElectorDocumentService.updateAnonymousElectorDocuments(eroId, updateAedDto)

            // Then
            assertThat(aed1.contactDetails!!.email).isEqualTo(newEmailAddress)
            assertThat(aed1.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
            assertThat(aed2.contactDetails!!.email).isEqualTo(newEmailAddress)
            assertThat(aed2.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
        }

        @Test
        fun `should skip updating AEDs that have passed the initial retention period`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCodes = listOf(aGssCode(), aGssCode())

            val originalEmailAddress = aValidEmailAddress()
            val originalPhoneNumber = aValidPhoneNumber()
            val aed1 = buildAnonymousElectorDocument(
                gssCode = gssCodes.first(),
                contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
            )
            val aed2InitialDataRemoved =
                buildAnonymousElectorDocument(
                    gssCode = gssCodes.first(),
                    sourceReference = aed1.sourceReference,
                ).also { it.removeInitialRetentionPeriodData() }

            val newEmailAddress = anotherValidEmailAddress()
            val updateAedDto = buildUpdateAnonymousElectorDocumentDto(
                sourceReference = aed1.sourceReference,
                email = newEmailAddress,
                phoneNumber = null
            )
            given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            ).willReturn(listOf(aed1, aed2InitialDataRemoved))
            TestLogAppender.reset()

            // When
            anonymousElectorDocumentService.updateAnonymousElectorDocuments(eroId, updateAedDto)

            // Then
            assertThat(aed1.contactDetails!!.email).isEqualTo(newEmailAddress)
            assertThat(aed1.contactDetails!!.phoneNumber).isEqualTo(originalPhoneNumber)
            assertThat(aed2InitialDataRemoved.contactDetails!!.email).isNull()
            assertThat(aed2InitialDataRemoved.contactDetails!!.phoneNumber).isNull()

            assertThat(
                TestLogAppender.hasLog(
                    "Skipping update for certificate ${aed2InitialDataRemoved.certificateNumber} as it has passed the initial retention period",
                    Level.INFO
                )
            ).isTrue
        }

        @Test
        fun `should not update AEDs if all certificates have expired`() {
            // Given
            val eroId = aValidRandomEroId()
            val aed1 = buildAnonymousElectorDocument()
                .also { it.removeInitialRetentionPeriodData() }
            val aed2 = buildAnonymousElectorDocument()
                .also { it.removeInitialRetentionPeriodData() }
            val updateAedDto = buildUpdateAnonymousElectorDocumentDto(
                sourceReference = aValidSourceReference()
            )

            given(eroService.lookupGssCodesForEro(any())).willReturn(listOf(aGssCode(), aGssCode()))
            given(
                anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(
                    any(),
                    any(),
                    any(),
                )
            ).willReturn(listOf(aed1, aed2))

            TestLogAppender.reset()

            // When
            val ex = catchThrowableOfType(UpdateAnonymousElectorDocumentAllInitialDataRemovedException::class.java) {
                anonymousElectorDocumentService.updateAnonymousElectorDocuments(
                    eroId,
                    updateAedDto
                )
            }

            // Then
            assertThat(ex.message).isEqualTo("All certificate for eroId = $eroId with sourceReference = ${updateAedDto.sourceReference} have pass the initial retention period and cannot be updated.")
        }
    }
}
