package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.dto.AddressFormat
import uk.gov.dluhc.printapi.dto.DeliveryClass
import uk.gov.dluhc.printapi.dto.SourceType
import uk.gov.dluhc.printapi.dto.aed.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.CertificateLanguageMapper
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.models.SourceType.ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildGenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildValidAddressDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildDtoCertificateDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAddress
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildApiCertificateDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateAnonymousElectorDocumentRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntity
import uk.gov.dluhc.printapi.dto.DeliveryAddressType as DeliveryAddressTypeDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatDto
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatApi
import uk.gov.dluhc.printapi.models.DeliveryAddressType as DeliveryAddressTypeApi

@ExtendWith(MockitoExtension::class)
class GenerateAnonymousElectorDocumentMapperTest {

    companion object {
        private const val FIXED_DATE_STRING = "2022-10-18"
        private val FIXED_DATE = LocalDate.parse(FIXED_DATE_STRING)
        private val FIXED_TIME = Instant.parse("${FIXED_DATE_STRING}T11:22:32.123Z")
        private val FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        private val ID_FIELDS_REGEX = ".*id"
    }

    @InjectMocks
    private lateinit var mapper: GenerateAnonymousElectorDocumentMapperImpl

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: AnonymousSupportingInformationFormatMapper

    @Mock
    private lateinit var deliveryAddressTypeMapper: DeliveryAddressTypeMapper

    @Mock
    private lateinit var idFactory: IdFactory

    @Spy
    private val clock: Clock = FIXED_CLOCK

    @Nested
    inner class ToAnonymousElectorDocument {

        @Test
        fun `should map GenerateAnonymousElectorDocumentDto to an AnonymousElectorDocument entity`() {
            // Given
            val dtoRequest =
                buildGenerateAnonymousElectorDocumentDto(
                    delivery = buildDtoCertificateDelivery(
                        deliveryAddressType = DeliveryAddressTypeDto.ERO_COLLECTION,
                        collectionReason = "Away from home"
                    )
                )
            val aedTemplateFilename = aValidAnonymousElectorDocumentTemplateFilename()
            val certificateNumber = aValidVacNumber()

            given(sourceTypeMapper.mapDtoToEntity(any())).willReturn(ANONYMOUS_ELECTOR_DOCUMENT)
            given(certificateLanguageMapper.mapDtoToEntity(any())).willReturn(CertificateLanguage.EN)
            given(supportingInformationFormatMapper.mapDtoToEntity(any())).willReturn(SupportingInformationFormatEntity.EASY_READ)
            given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressTypeEntity.ERO_COLLECTION)
            given(idFactory.vacNumber()).willReturn(certificateNumber)

            val expected = with(dtoRequest) {
                AnonymousElectorDocument(
                    id = null,
                    gssCode = gssCode,
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    certificateLanguage = CertificateLanguageEntity.EN,
                    supportingInformationFormat = SupportingInformationFormatEntity.EASY_READ,
                    certificateNumber = certificateNumber,
                    photoLocationArn = photoLocation,
                    electoralRollNumber = electoralRollNumber,
                    issueDate = FIXED_DATE,
                    aedTemplateFilename = aedTemplateFilename,
                    requestDateTime = FIXED_TIME,
                    contactDetails = AedContactDetails(
                        firstName = firstName,
                        middleNames = middleNames,
                        surname = surname,
                        email = email,
                        phoneNumber = phoneNumber,
                        address = with(registeredAddress) {
                            buildAddress(
                                street = street,
                                postcode = postcode,
                                property = property,
                                locality = locality,
                                town = town,
                                area = area,
                                uprn = uprn
                            )
                        },
                    ),
                    userId = userId,
                    statusHistory = mutableListOf(
                        AnonymousElectorDocumentStatus(
                            status = AnonymousElectorDocumentStatus.Status.PRINTED,
                            eventDateTime = FIXED_TIME
                        )
                    ),
                    delivery = with(delivery) {
                        buildDelivery(
                            addressee = addressee,
                            address = with(deliveryAddress) {
                                buildAddress(
                                    street = street,
                                    postcode = postcode,
                                    property = property,
                                    locality = locality,
                                    town = town,
                                    area = area,
                                    uprn = uprn,
                                )
                            },
                            deliveryClass = uk.gov.dluhc.printapi.database.entity.DeliveryClass.STANDARD,
                            deliveryAddressType = DeliveryAddressTypeEntity.ERO_COLLECTION,
                            collectionReason = collectionReason,
                            addressFormat = uk.gov.dluhc.printapi.database.entity.AddressFormat.UK,
                        )
                    }

                )
            }

            // When
            val actual = mapper.toAnonymousElectorDocument(dtoRequest, aedTemplateFilename)

            // Then
            assertThat(actual).usingRecursiveComparison().ignoringFieldsMatchingRegexes(ID_FIELDS_REGEX).isEqualTo(expected)
            verify(sourceTypeMapper).mapDtoToEntity(dtoRequest.sourceType)
            verify(certificateLanguageMapper).mapDtoToEntity(dtoRequest.certificateLanguage)
            verify(supportingInformationFormatMapper).mapDtoToEntity(dtoRequest.supportingInformationFormat)
            verify(idFactory).vacNumber()
            verify(deliveryAddressTypeMapper).mapDtoToEntity(DeliveryAddressTypeDto.ERO_COLLECTION)
            verifyNoMoreInteractions(sourceTypeMapper, certificateLanguageMapper, supportingInformationFormatMapper, idFactory, deliveryAddressTypeMapper)
        }
    }

    @Nested
    inner class ToAedContactDetailsEntity {

        @Test
        fun `should map to AedContactDetails entity given GenerateAnonymousElectorDocumentDto`() {
            // Given
            val request = buildGenerateAnonymousElectorDocumentDto()
            val expected = with(request) {
                AedContactDetails(
                    firstName = firstName,
                    middleNames = middleNames,
                    surname = surname,
                    email = email,
                    phoneNumber = phoneNumber,
                    address = with(registeredAddress) {
                        buildAddress(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    },
                )
            }

            // When
            val actual = mapper.toAedContactDetailsEntity(request)

            // Then
            assertThat(actual).usingRecursiveComparison().ignoringFieldsMatchingRegexes(ID_FIELDS_REGEX).isEqualTo(expected)
        }
    }

    @Nested
    inner class FromDeliveryDtoToDeliveryEntity {

        @Test
        fun `should map to Delivery entity given CertificateDelivery Dto`() {
            // Given
            val dto = buildDtoCertificateDelivery(
                deliveryAddressType = DeliveryAddressTypeDto.ERO_COLLECTION,
                collectionReason = "Away from home"
            )
            given(deliveryAddressTypeMapper.mapDtoToEntity(any())).willReturn(DeliveryAddressTypeEntity.ERO_COLLECTION)

            val expected = with(dto) {
                buildDelivery(
                    addressee = addressee,
                    addressFormat = uk.gov.dluhc.printapi.database.entity.AddressFormat.UK,
                    deliveryClass = uk.gov.dluhc.printapi.database.entity.DeliveryClass.STANDARD,
                    deliveryAddressType = DeliveryAddressTypeEntity.ERO_COLLECTION,
                    collectionReason = collectionReason,
                    address = with(deliveryAddress) {
                        buildAddress(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    },
                )
            }

            // When
            val actual = mapper.fromDeliveryDtoToDeliveryEntity(dto)

            // Then
            assertThat(actual).usingRecursiveComparison().ignoringFieldsMatchingRegexes(ID_FIELDS_REGEX).isEqualTo(expected)
            verify(deliveryAddressTypeMapper).mapDtoToEntity(DeliveryAddressTypeDto.ERO_COLLECTION)
        }
    }

    @Nested
    inner class ToGenerateAnonymousElectorDocumentDto {

        @Test
        fun `should map to GenerateAnonymousElectorDocumentDto DTO given API Request`() {
            // Given
            val userId = aValidUserId()
            val apiRequest = buildGenerateAnonymousElectorDocumentRequest(
                sourceType = ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT,
                certificateLanguage = uk.gov.dluhc.printapi.models.CertificateLanguage.EN,
                supportingInformationFormat = AnonymousSupportingInformationFormatApi.LARGE_MINUS_PRINT,
                delivery = buildApiCertificateDelivery()
            )

            given(sourceTypeMapper.mapApiToDto(any())).willReturn(SourceType.ANONYMOUS_ELECTOR_DOCUMENT)
            given(certificateLanguageMapper.mapApiToDto(any())).willReturn(uk.gov.dluhc.printapi.dto.CertificateLanguage.EN)
            given(supportingInformationFormatMapper.mapApiToDto(any())).willReturn(AnonymousSupportingInformationFormatDto.LARGE_PRINT)
            given(deliveryAddressTypeMapper.mapApiToDto(any())).willReturn(DeliveryAddressTypeDto.REGISTERED)

            val expected = GenerateAnonymousElectorDocumentDto(
                gssCode = apiRequest.gssCode,
                sourceType = SourceType.ANONYMOUS_ELECTOR_DOCUMENT,
                sourceReference = apiRequest.sourceReference,
                applicationReference = apiRequest.applicationReference,
                electoralRollNumber = apiRequest.electoralRollNumber,
                photoLocation = apiRequest.photoLocation,
                certificateLanguage = uk.gov.dluhc.printapi.dto.CertificateLanguage.EN,
                supportingInformationFormat = AnonymousSupportingInformationFormatDto.LARGE_PRINT,
                firstName = apiRequest.firstName,
                middleNames = apiRequest.middleNames,
                surname = apiRequest.surname,
                email = apiRequest.email,
                phoneNumber = apiRequest.phoneNumber,
                registeredAddress = with(apiRequest.registeredAddress) {
                    buildValidAddressDto(
                        property = property,
                        street = street,
                        town = town,
                        area = area,
                        locality = locality,
                        uprn = uprn,
                        postcode = postcode
                    )
                },
                userId = userId,
                delivery = buildDtoCertificateDelivery(
                    deliveryClass = DeliveryClass.STANDARD,
                    deliveryAddressType = DeliveryAddressTypeDto.REGISTERED,
                    collectionReason = null,
                    addressee = apiRequest.delivery.addressee,
                    addressFormat = AddressFormat.UK,
                    deliveryAddress = with(apiRequest.delivery.deliveryAddress) {
                        buildValidAddressDto(
                            property = property,
                            street = street,
                            town = town,
                            area = area,
                            locality = locality,
                            uprn = uprn,
                            postcode = postcode
                        )
                    }
                )
            )

            // When
            val actual = mapper.toGenerateAnonymousElectorDocumentDto(apiRequest, userId)

            // Then
            assertThat(actual).isEqualTo(expected)
            verify(sourceTypeMapper).mapApiToDto(ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT)
            verify(certificateLanguageMapper).mapApiToDto(uk.gov.dluhc.printapi.models.CertificateLanguage.EN)
            verify(supportingInformationFormatMapper).mapApiToDto(AnonymousSupportingInformationFormatApi.LARGE_MINUS_PRINT)
            verify(deliveryAddressTypeMapper).mapApiToDto(DeliveryAddressTypeApi.REGISTERED)
            verifyNoMoreInteractions(sourceTypeMapper, certificateLanguageMapper, supportingInformationFormatMapper, deliveryAddressTypeMapper)
        }
    }
}
