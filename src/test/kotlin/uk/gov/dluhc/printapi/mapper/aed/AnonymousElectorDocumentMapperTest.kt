package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType.ERO_COLLECTION
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.dto.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.factory.UrlFactory
import uk.gov.dluhc.printapi.mapper.CertificateLanguageMapper
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.mapper.InstantMapper
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.models.CertificateLanguage
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousElectorDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildValidAddressDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAddress
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElectorApi
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElectorDocumentApi
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildValidAddress
import uk.gov.dluhc.printapi.testsupport.testdata.zip.anAedPhotoUrl
import uk.gov.dluhc.printapi.dto.CertificateLanguage as DtoCertificateLanguage
import uk.gov.dluhc.printapi.dto.DeliveryAddressType as DtoDeliveryAddressType
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDocumentStatus as DtoAnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.aed.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatDtoEnum
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatApiEnum

@ExtendWith(MockitoExtension::class)
class AnonymousElectorDocumentMapperTest {

    @InjectMocks
    private lateinit var mapper: AnonymousElectorDocumentMapperImpl

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: AnonymousSupportingInformationFormatMapper

    @Mock
    private lateinit var deliveryAddressTypeMapper: DeliveryAddressTypeMapper

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Mock
    private lateinit var urlFactory: UrlFactory

    @Nested
    inner class MapToApiAnonymousElectorDocument {
        @Test
        fun `should map AnonymousElectorDocumentDto to an AnonymousElectorDocument API model`() {
            // Given
            val dtoRequest = buildAnonymousElectorDocumentDto(
                supportingInformationFormat = AnonymousSupportingInformationFormatDtoEnum.EASY_READ,
                deliveryAddressType = DtoDeliveryAddressType.ERO_COLLECTION,
                collectionReason = "Currently away"
            )
            val requestDateTime = aValidGeneratedDateTime()

            given(certificateLanguageMapper.mapDtoToApi(any())).willReturn(CertificateLanguage.EN)
            given(supportingInformationFormatMapper.mapDtoToApi(any())).willReturn(AnonymousSupportingInformationFormatApiEnum.EASY_MINUS_READ)
            given(deliveryAddressTypeMapper.mapDtoToApi(any())).willReturn(DeliveryAddressType.ERO_MINUS_COLLECTION)
            given(instantMapper.toOffsetDateTime(any())).willReturn(requestDateTime)

            val eroId = aValidEroId()
            val expectedPhotoUrl = anAedPhotoUrl(eroId = eroId, sourceReference = dtoRequest.sourceReference)
            given(urlFactory.createPhotoUrl(any(), any(), any())).willReturn(expectedPhotoUrl)

            val expected = with(dtoRequest) {
                buildAnonymousElectorDocumentApi(
                    certificateNumber = certificateNumber,
                    electoralRollNumber = electoralRollNumber,
                    gssCode = gssCode,
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    certificateLanguage = CertificateLanguage.EN,
                    supportingInformationFormat = AnonymousSupportingInformationFormatApiEnum.EASY_MINUS_READ,
                    deliveryAddressType = DeliveryAddressType.ERO_MINUS_COLLECTION,
                    collectionReason = collectionReason,
                    elector = with(elector) {
                        buildAnonymousElectorApi(
                            firstName = firstName,
                            middleNames = middleNames,
                            surname = surname,
                            addressee = addressee,
                            registeredAddress = registeredAddress?.let {
                                buildValidAddress(
                                    street = it.street,
                                    postcode = it.postcode,
                                    property = it.property,
                                    locality = it.locality,
                                    town = it.town,
                                    area = it.area,
                                    uprn = it.uprn,
                                )
                            },
                            email = email,
                            phoneNumber = phoneNumber,
                        )
                    },
                    photoUrl = expectedPhotoUrl,
                    issueDate = issueDate,
                    status = AnonymousElectorDocumentStatus.PRINTED,
                    userId = userId,
                    dateTime = requestDateTime,
                )
            }

            // When
            val actual = mapper.mapToApiAnonymousElectorDocument(dtoRequest, eroId)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(certificateLanguageMapper).mapDtoToApi(dtoRequest.certificateLanguage)
            verify(supportingInformationFormatMapper).mapDtoToApi(dtoRequest.supportingInformationFormat!!)
            verify(deliveryAddressTypeMapper).mapDtoToApi(DtoDeliveryAddressType.ERO_COLLECTION)
            verify(instantMapper).toOffsetDateTime(dtoRequest.requestDateTime)
            verifyNoMoreInteractions(
                certificateLanguageMapper, supportingInformationFormatMapper,
                deliveryAddressTypeMapper, instantMapper
            )
            verify(urlFactory).createPhotoUrl(eroId, ANONYMOUS_ELECTOR_DOCUMENT, dtoRequest.sourceReference)
        }
    }

    @Nested
    inner class MapToAnonymousElectorDocumentDto {
        @Test
        fun `should map AnonymousElectorDocument entity to an AnonymousElectorDocumentDto`() {
            // Given
            val entityRequest = buildAnonymousElectorDocument(
                supportingInformationFormat = SupportingInformationFormat.BRAILLE,
                contactDetails = buildAedContactDetails(
                    firstName = "John",
                    middleNames = "J",
                    surname = "Bloggs"
                ),
                delivery = buildAedDelivery(deliveryAddressType = ERO_COLLECTION, collectionReason = "Away from home")
            )
            given(certificateLanguageMapper.mapEntityToDto(any())).willReturn(DtoCertificateLanguage.EN)
            given(supportingInformationFormatMapper.mapEntityToDto(any())).willReturn(AnonymousSupportingInformationFormatDtoEnum.BRAILLE)
            given(deliveryAddressTypeMapper.mapEntityToDto(any())).willReturn(DtoDeliveryAddressType.ERO_COLLECTION)

            val expected = with(entityRequest) {
                buildAnonymousElectorDocumentDto(
                    certificateNumber = certificateNumber,
                    electoralRollNumber = electoralRollNumber,
                    gssCode = gssCode,
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    certificateLanguage = DtoCertificateLanguage.EN,
                    supportingInformationFormat = AnonymousSupportingInformationFormatDtoEnum.BRAILLE,
                    deliveryAddressType = DtoDeliveryAddressType.ERO_COLLECTION,
                    collectionReason = delivery!!.collectionReason,
                    elector = with(contactDetails!!) {
                        buildAnonymousElectorDto(
                            firstName = firstName,
                            middleNames = middleNames,
                            surname = surname,
                            addressee = "John J Bloggs",
                            registeredAddress = with(address!!) {
                                buildValidAddressDto(
                                    street = street!!,
                                    postcode = postcode!!,
                                    property = property,
                                    locality = locality,
                                    town = town,
                                    area = area,
                                    uprn = uprn,
                                )
                            },
                            email = email,
                            phoneNumber = phoneNumber
                        )
                    },
                    photoLocationArn = photoLocationArn,
                    issueDate = issueDate,
                    status = DtoAnonymousElectorDocumentStatus.PRINTED,
                    userId = userId,
                    requestDateTime = requestDateTime,
                )
            }

            // When
            val actual = mapper.mapToAnonymousElectorDocumentDto(entityRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(certificateLanguageMapper).mapEntityToDto(entityRequest.certificateLanguage)
            verify(supportingInformationFormatMapper).mapEntityToDto(entityRequest.supportingInformationFormat!!)
            verify(deliveryAddressTypeMapper).mapEntityToDto(ERO_COLLECTION)
            verifyNoMoreInteractions(certificateLanguageMapper, supportingInformationFormatMapper, deliveryAddressTypeMapper)
            verifyNoInteractions(instantMapper)
        }

        @Test
        fun `should map expired AnonymousElectorDocument entity to an AnonymousElectorDocumentDto`() {
            // Given
            val entityRequest = buildAnonymousElectorDocument(
                contactDetails = buildAedContactDetails(
                    firstName = "John",
                    middleNames = "J",
                    surname = "Bloggs"
                ),
            ).also { it.removeInitialRetentionPeriodData() }
            given(certificateLanguageMapper.mapEntityToDto(any())).willReturn(DtoCertificateLanguage.EN)

            val expected = with(entityRequest) {
                buildAnonymousElectorDocumentDto(
                    certificateNumber = certificateNumber,
                    electoralRollNumber = electoralRollNumber,
                    gssCode = gssCode,
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    certificateLanguage = DtoCertificateLanguage.EN,
                    supportingInformationFormat = null,
                    deliveryAddressType = null,
                    collectionReason = delivery?.collectionReason,
                    elector = with(contactDetails!!) {
                        buildAnonymousElectorDto(
                            firstName = firstName,
                            middleNames = middleNames,
                            surname = surname,
                            addressee = "John J Bloggs",
                            registeredAddress = null,
                            email = null,
                            phoneNumber = null,
                        )
                    },
                    photoLocationArn = photoLocationArn,
                    issueDate = issueDate,
                    status = DtoAnonymousElectorDocumentStatus.EXPIRED,
                    userId = userId,
                    requestDateTime = requestDateTime,
                )
            }

            // When
            val actual = mapper.mapToAnonymousElectorDocumentDto(entityRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(certificateLanguageMapper).mapEntityToDto(entityRequest.certificateLanguage)
            verify(supportingInformationFormatMapper).mapEntityToDto(anyOrNull())
            verify(deliveryAddressTypeMapper).mapEntityToDto(anyOrNull())
            verifyNoMoreInteractions(certificateLanguageMapper, supportingInformationFormatMapper, deliveryAddressTypeMapper)
            verifyNoInteractions(instantMapper)
        }
    }

    @Nested
    inner class MapFromContactDetailsToElectorDto {

        @Test
        fun `should map AedContactDetails entity to AnonymousElectorDto given all names present`() {
            // Given
            val aedContactDetailsEntity =
                buildAedContactDetails(firstName = "Joe", middleNames = "John", surname = "Bloggs")
            val expected = with(aedContactDetailsEntity) {
                buildAnonymousElectorDto(
                    firstName = firstName,
                    middleNames = middleNames,
                    surname = surname,
                    addressee = "Joe John Bloggs",
                    registeredAddress = with(address!!) {
                        buildValidAddressDto(
                            street = street!!,
                            postcode = postcode!!,
                            property = property!!,
                            locality = locality!!,
                            town = town!!,
                            area = area!!,
                            uprn = uprn!!
                        )
                    },
                    email = email,
                    phoneNumber = phoneNumber
                )
            }

            // When
            val actual = mapper.mapFromContactDetailsToElectorDto(aedContactDetailsEntity)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }

        @Test
        fun `should map AedContactDetails entity to registeredAddress of AnonymousElectorDto given only mandatory address fields present`() {
            // Given
            val aedContactDetailsEntity = buildAedContactDetails(
                address = buildAddress(
                    property = null, locality = null, town = null, area = null, uprn = null,
                )
            )
            val expectedAddressDto = with(aedContactDetailsEntity.address!!) {
                buildValidAddressDto(
                    street = street!!,
                    postcode = postcode!!,
                    property = null, locality = null, town = null, area = null, uprn = null,
                )
            }

            // When
            val actual = mapper.mapFromContactDetailsToElectorDto(aedContactDetailsEntity)

            // Then
            assertThat(actual.registeredAddress).usingRecursiveComparison().isEqualTo(expectedAddressDto)
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = ["  "])
        fun `should map AedContactDetails entity names to Assignee for empty or null middleNames`(
            middleNames: String?
        ) {
            // Given
            val aedContactDetailsEntity =
                buildAedContactDetails(firstName = "Joe", middleNames = middleNames, surname = "Bloggs")

            // When
            val actual = mapper.mapFromContactDetailsToElectorDto(aedContactDetailsEntity)

            // Then
            assertThat(actual.addressee).isEqualTo("Joe Bloggs")
        }
    }
}
