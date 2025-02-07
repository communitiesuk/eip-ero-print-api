package uk.gov.dluhc.printapi.mapper.aed

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
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.dto.DeliveryAddressType.ERO_COLLECTION
import uk.gov.dluhc.printapi.dto.DeliveryAddressType.REGISTERED
import uk.gov.dluhc.printapi.dto.aed.ReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.models.DeliveryAddressType.ERO_MINUS_COLLECTION
import uk.gov.dluhc.printapi.service.ElectorDocumentRemovalDateResolver
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.deepCopy
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildReIssueAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.aTemplateFilename
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ReIssueAnonymousElectorDocumentMapperTest {

    companion object {
        private const val FIXED_DATE_IN_OCT_STRING = "2022-10-18"
        private val FIXED_DATE_IN_OCT = LocalDate.parse(FIXED_DATE_IN_OCT_STRING)

        private const val FIXED_DATE_IN_MAR_STRING = "2022-03-18"
        private val FIXED_DATE_IN_MAR = LocalDate.parse(FIXED_DATE_IN_MAR_STRING)

        private val FIXED_TIME = Instant.parse("${FIXED_DATE_IN_OCT_STRING}T11:22:32.123Z")
        private val IGNORED_FIELDS =
            arrayOf(".*id", ".*dateCreated", ".*createdBy", ".*dateUpdated", ".*updatedBy", ".*version")
    }

    @InjectMocks
    private lateinit var mapper: ReIssueAnonymousElectorDocumentMapperImpl

    @Mock
    private lateinit var deliveryAddressTypeMapper: DeliveryAddressTypeMapper

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var aedMappingHelper: AedMappingHelper

    @Mock
    private lateinit var electorDocumentRemovalDateResolver: ElectorDocumentRemovalDateResolver

    @Test
    fun `should map to ReIssueAnonymousElectorDocumentDto DTO given API Request`() {
        // Given
        val userId = aValidUserId()
        val sourceReference = aValidSourceReference()
        val electoralRollNumber = aValidElectoralRollNumber()
        val deliveryAddressType = ERO_MINUS_COLLECTION

        val apiRequest = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = electoralRollNumber,
            deliveryAddressType = deliveryAddressType
        )

        given(deliveryAddressTypeMapper.mapApiToDto(any())).willReturn(ERO_COLLECTION)

        val expected = ReIssueAnonymousElectorDocumentDto(
            userId = userId,
            sourceReference = sourceReference,
            electoralRollNumber = electoralRollNumber,
            deliveryAddressType = ERO_COLLECTION
        )

        // When
        val actual = mapper.toReIssueAnonymousElectorDocumentDto(apiRequest, userId)

        // Then
        assertThat(actual).isEqualTo(expected)
        verify(deliveryAddressTypeMapper).mapApiToDto(ERO_MINUS_COLLECTION)
    }

    @Test
    fun `should map to new Anonymous Elector Document given a previous AED`() {
        // Given
        val sourceReference = aValidSourceReference()
        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"
        val previousAed = buildAnonymousElectorDocument(
            persisted = true,
            sourceReference = sourceReference,
            certificateNumber = aValidVacNumber(),
            electoralRollNumber = originalElectoralRollNumber,
            delivery = buildAedDelivery(
                deliveryAddressType = DeliveryAddressType.ERO_COLLECTION,
                collectionReason = "There is a postal strike"
            ),
        ).apply {
            createdBy = "some-user"
            dateCreated = Instant.now().minus(1, ChronoUnit.DAYS)
            version = 0
            with(contactDetails!!) {
                id = UUID.randomUUID()
                createdBy = "some-user"
                dateCreated = Instant.now().minus(1, ChronoUnit.DAYS)
                updatedBy = "some-other-user"
                dateUpdated = Instant.now()
                version = 1
            }
        }
        val templateFilename = aTemplateFilename()

        val newCertificateNumber = aValidVacNumber()
        given(idFactory.vacNumber()).willReturn(newCertificateNumber)
        given(aedMappingHelper.requestDateTime()).willReturn(FIXED_TIME)
        given(aedMappingHelper.issueDate()).willReturn(FIXED_DATE_IN_OCT)
        val expectedStatusHistory = listOf(
            AnonymousElectorDocumentStatus(
                status = AnonymousElectorDocumentStatus.Status.PRINTED,
                eventDateTime = FIXED_TIME
            )
        )
        given(aedMappingHelper.statusHistory(any())).willReturn(expectedStatusHistory)

        val userId = aValidUserId()
        val newElectoralRollNumber = aValidElectoralRollNumber()
        val dto = buildReIssueAnonymousElectorDocumentDto(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
            deliveryAddressType = REGISTERED,
            userId = userId
        )

        given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        val expected = previousAed.deepCopy().apply {
            certificateNumber = newCertificateNumber
            requestDateTime = FIXED_TIME
            issueDate = FIXED_DATE_IN_OCT
            statusHistory = expectedStatusHistory.toMutableList()
            electoralRollNumber = newElectoralRollNumber
            delivery?.deliveryAddressType = DeliveryAddressType.REGISTERED
            delivery?.collectionReason = "There is a postal strike"
            this.userId = userId
        }

        // When
        val actual = mapper.toNewAnonymousElectorDocument(previousAed, dto, templateFilename)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFieldsMatchingRegexes(*IGNORED_FIELDS).isEqualTo(expected)
        assertThat(actual.id).isNull()
        assertThat(actual.delivery!!.id).isNull()
        assertThat(actual.delivery!!.createdBy).isNull()
        assertThat(actual.delivery!!.dateCreated).isNull()
        assertThat(actual.delivery!!.version).isEqualTo(0L)
        assertThat(actual.delivery!!.address!!.id).isNull()
        assertThat(actual.delivery!!.address!!.dateCreated).isNull()
        assertThat(actual.delivery!!.address!!.createdBy).isNull()
        assertThat(actual.delivery!!.address!!.version).isEqualTo(0L)
        assertThat(actual.contactDetails!!.id).isNull()
        assertThat(actual.contactDetails!!.dateCreated).isNull()
        assertThat(actual.contactDetails!!.createdBy).isNull()
        assertThat(actual.contactDetails!!.dateUpdated).isNull()
        assertThat(actual.contactDetails!!.updatedBy).isNull()
        assertThat(actual.contactDetails!!.version).isEqualTo(0L)
        assertThat(actual.contactDetails!!.address!!.id).isNull()
        assertThat(actual.contactDetails!!.address!!.dateCreated).isNull()
        assertThat(actual.contactDetails!!.address!!.createdBy).isNull()
        assertThat(actual.contactDetails!!.address!!.version).isEqualTo(0L)
        verify(idFactory).vacNumber()
        verify(aedMappingHelper).issueDate()
        verify(aedMappingHelper).requestDateTime()
        verify(aedMappingHelper).statusHistory(AnonymousElectorDocumentStatus.Status.PRINTED)
        verify(deliveryAddressTypeMapper).mapDtoToEntity(REGISTERED)
    }

    @Test
    fun `should map to new Anonymous Elector Document with provided deliveryAddressType, given an AED without delivery information`() {
        // Given
        val previousAed = buildAnonymousElectorDocument(delivery = null)
        given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressType.ERO_COLLECTION)

        given(idFactory.vacNumber()).willReturn(aValidVacNumber())
        given(aedMappingHelper.requestDateTime()).willReturn(FIXED_TIME)
        given(aedMappingHelper.issueDate()).willReturn(FIXED_DATE_IN_OCT)
        given(aedMappingHelper.statusHistory(any())).willReturn(listOf())

        // When
        val actual = mapper.toNewAnonymousElectorDocument(
            previousAed,
            buildReIssueAnonymousElectorDocumentDto(),
            aTemplateFilename()
        )

        // Then
        assertThat(actual.delivery?.deliveryAddressType).usingRecursiveComparison().isEqualTo(ERO_COLLECTION)
    }

    @Test
    fun `should set initialRetentionRemovalDate to be null if not set for previous AED`() {
        // Given
        val sourceReference = aValidSourceReference()
        val previousAed = buildAnonymousElectorDocument(
            sourceReference = sourceReference,
            initialRetentionRemovalDate = null,
        )
        val templateFilename = aTemplateFilename()
        val newCertificateNumber = aValidVacNumber()
        given(idFactory.vacNumber()).willReturn(newCertificateNumber)
        given(aedMappingHelper.requestDateTime()).willReturn(FIXED_TIME)
        given(aedMappingHelper.issueDate()).willReturn(FIXED_DATE_IN_OCT)

        val dto = buildReIssueAnonymousElectorDocumentDto(sourceReference = sourceReference)

        given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        // When
        val actual = mapper.toNewAnonymousElectorDocument(previousAed, dto, templateFilename)

        // Then
        assertThat(actual.initialRetentionRemovalDate).isNull()
        verifyNoInteractions(electorDocumentRemovalDateResolver)
    }

    @Test
    fun `should set initialRetentionRemovalDate based on new issue date if already set for previous AED`() {
        // Given
        val sourceReference = aValidSourceReference()

        val previousRemovalTime = LocalDate.parse("2023-01-01")
        val previousAed = buildAnonymousElectorDocument(
            sourceReference = sourceReference,
            initialRetentionRemovalDate = previousRemovalTime,
        )
        val templateFilename = aTemplateFilename()
        val newCertificateNumber = aValidVacNumber()
        given(idFactory.vacNumber()).willReturn(newCertificateNumber)
        given(aedMappingHelper.requestDateTime()).willReturn(FIXED_TIME)
        given(aedMappingHelper.issueDate()).willReturn(FIXED_DATE_IN_OCT)

        val dto = buildReIssueAnonymousElectorDocumentDto(sourceReference = sourceReference)

        given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        val newRemovalTime = FIXED_DATE_IN_OCT.plusMonths(15)
        given(electorDocumentRemovalDateResolver.getAedInitialRetentionPeriodRemovalDate(FIXED_DATE_IN_OCT)).willReturn(newRemovalTime)

        // When
        val actual = mapper.toNewAnonymousElectorDocument(previousAed, dto, templateFilename)

        // Then
        assertThat(actual.initialRetentionRemovalDate).isEqualTo(newRemovalTime)
        verify(electorDocumentRemovalDateResolver).getAedInitialRetentionPeriodRemovalDate(FIXED_DATE_IN_OCT)
    }

    @Test
    fun `should set finalRetentionRemovalDate to be null if not set for previous AED`() {
        // Given
        val sourceReference = aValidSourceReference()
        val previousAed = buildAnonymousElectorDocument(
            sourceReference = sourceReference,
            finalRetentionRemovalDate = null,
        )
        val templateFilename = aTemplateFilename()
        val newCertificateNumber = aValidVacNumber()
        given(idFactory.vacNumber()).willReturn(newCertificateNumber)
        given(aedMappingHelper.requestDateTime()).willReturn(FIXED_TIME)
        given(aedMappingHelper.issueDate()).willReturn(FIXED_DATE_IN_OCT)

        val dto = buildReIssueAnonymousElectorDocumentDto(sourceReference = sourceReference)

        given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        // When
        val actual = mapper.toNewAnonymousElectorDocument(previousAed, dto, templateFilename)

        // Then
        assertThat(actual.finalRetentionRemovalDate).isNull()
        verifyNoInteractions(electorDocumentRemovalDateResolver)
    }

    @Test
    fun `should set finalRetentionRemovalDate to 9 years from new issue date if already set for previous AED and issued in first half of the year`() {
        // Given
        val sourceReference = aValidSourceReference()

        val previousRemovalTime = LocalDate.parse("2023-01-01")
        val previousAed = buildAnonymousElectorDocument(
            sourceReference = sourceReference,
            finalRetentionRemovalDate = previousRemovalTime,
        )
        val templateFilename = aTemplateFilename()
        val newCertificateNumber = aValidVacNumber()
        given(idFactory.vacNumber()).willReturn(newCertificateNumber)
        given(aedMappingHelper.requestDateTime()).willReturn(FIXED_TIME)
        given(aedMappingHelper.issueDate()).willReturn(FIXED_DATE_IN_MAR)

        val dto = buildReIssueAnonymousElectorDocumentDto(sourceReference = sourceReference)

        given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        val newRemovalTime = FIXED_DATE_IN_MAR.plusYears(9)
        given(electorDocumentRemovalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(FIXED_DATE_IN_MAR)).willReturn(newRemovalTime)

        // When
        val actual = mapper.toNewAnonymousElectorDocument(previousAed, dto, templateFilename)

        // Then
        assertThat(actual.finalRetentionRemovalDate).isEqualTo(newRemovalTime)
        verify(electorDocumentRemovalDateResolver).getElectorDocumentFinalRetentionPeriodRemovalDate(FIXED_DATE_IN_MAR)
    }

    @Test
    fun `should set finalRetentionRemovalDate to 10 years from new issue date if already set for previous AED and issued in second half of the year`() {
        // Given
        val sourceReference = aValidSourceReference()

        val previousRemovalTime = LocalDate.parse("2023-01-01")
        val previousAed = buildAnonymousElectorDocument(
            sourceReference = sourceReference,
            finalRetentionRemovalDate = previousRemovalTime,
        )
        val templateFilename = aTemplateFilename()
        val newCertificateNumber = aValidVacNumber()
        given(idFactory.vacNumber()).willReturn(newCertificateNumber)
        given(aedMappingHelper.requestDateTime()).willReturn(FIXED_TIME)
        given(aedMappingHelper.issueDate()).willReturn(FIXED_DATE_IN_OCT)

        val dto = buildReIssueAnonymousElectorDocumentDto(sourceReference = sourceReference)

        given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        val newRemovalTime = FIXED_DATE_IN_OCT.plusYears(10)
        given(electorDocumentRemovalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(FIXED_DATE_IN_OCT)).willReturn(newRemovalTime)

        // When
        val actual = mapper.toNewAnonymousElectorDocument(previousAed, dto, templateFilename)

        // Then
        assertThat(actual.finalRetentionRemovalDate).isEqualTo(newRemovalTime)
        verify(electorDocumentRemovalDateResolver).getElectorDocumentFinalRetentionPeriodRemovalDate(FIXED_DATE_IN_OCT)
    }
}
