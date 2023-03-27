package uk.gov.dluhc.printapi.mapper

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
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType.REGISTERED
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.models.CertificateLanguage
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.models.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildAnonymousElectorDocumentSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildAnonymousElectorDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildValidAddressDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElector
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildValidAddress
import uk.gov.dluhc.printapi.dto.AnonymousElectorDocumentStatus as DtoAnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.CertificateLanguage as DtoCertificateLanguage
import uk.gov.dluhc.printapi.dto.DeliveryAddressType as DtoDeliveryAddressType
import uk.gov.dluhc.printapi.dto.SupportingInformationFormat as DtoSupportingInformationFormat

@ExtendWith(MockitoExtension::class)
class AnonymousElectorSummaryMapperTest {

    @InjectMocks
    private lateinit var mapper: AnonymousElectorSummaryMapperImpl

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: SupportingInformationFormatMapper

    @Mock
    private lateinit var deliveryAddressTypeMapper: DeliveryAddressTypeMapper

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Nested
    inner class MapToApiAnonymousElectorDocumentSummary {
        @Test
        fun `should map AnonymousElectorDocumentSummaryDto to an AnonymousElectorDocumentSummary API model`() {
            // Given
            val dtoRequest = buildAnonymousElectorDocumentSummaryDto()
            val documentGeneratedTime = aValidGeneratedDateTime()

            given(certificateLanguageMapper.mapDtoToApi(any())).willReturn(CertificateLanguage.EN)
            given(supportingInformationFormatMapper.mapDtoToApi(any())).willReturn(SupportingInformationFormat.STANDARD)
            given(deliveryAddressTypeMapper.mapDtoToApi(any())).willReturn(DeliveryAddressType.REGISTERED)
            given(instantMapper.toOffsetDateTime(any())).willReturn(documentGeneratedTime)

            val expected = with(dtoRequest) {
                buildAnonymousElectorDocumentSummary(
                    certificateNumber = certificateNumber,
                    electoralRollNumber = electoralRollNumber,
                    gssCode = gssCode,
                    certificateLanguage = CertificateLanguage.EN,
                    supportingInformationFormat = SupportingInformationFormat.STANDARD,
                    deliveryAddressType = DeliveryAddressType.REGISTERED,
                    elector = with(elector) {
                        buildAnonymousElector(
                            addressee = addressee,
                            registeredAddress = with(registeredAddress) {
                                buildValidAddress(
                                    street = street,
                                    postcode = postcode,
                                    property = property,
                                    locality = locality,
                                    town = town,
                                    area = area,
                                    uprn = uprn,
                                )
                            }
                        )
                    },
                    photoLocation = photoLocation,
                    issueDate = issueDate,
                    status = AnonymousElectorDocumentStatus.PRINTED,
                    userId = userId,
                    dateTime = documentGeneratedTime,
                )
            }

            // When
            val actual = mapper.mapToApiAnonymousElectorDocumentSummary(dtoRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(certificateLanguageMapper).mapDtoToApi(dtoRequest.certificateLanguage)
            verify(supportingInformationFormatMapper).mapDtoToApi(dtoRequest.supportingInformationFormat)
            verify(deliveryAddressTypeMapper).mapDtoToApi(DtoDeliveryAddressType.REGISTERED)
            verify(instantMapper).toOffsetDateTime(dtoRequest.dateTime)
            verifyNoMoreInteractions(
                certificateLanguageMapper, supportingInformationFormatMapper,
                deliveryAddressTypeMapper, instantMapper
            )
        }
    }

    @Nested
    inner class MapToAnonymousElectorDocumentSummaryDto {
        @Test
        fun `should map AnonymousElectorDocument entity to an AnonymousElectorDocumentSummaryDto`() {
            // Given
            val entityRequest = buildAnonymousElectorDocument(
                contactDetails = buildAedContactDetails(
                    firstName = "John",
                    middleNames = "J",
                    surname = "Bloggs"
                )
            )
            given(certificateLanguageMapper.mapEntityToDto(any())).willReturn(DtoCertificateLanguage.EN)
            given(supportingInformationFormatMapper.mapEntityToDto(any())).willReturn(DtoSupportingInformationFormat.STANDARD)
            given(deliveryAddressTypeMapper.mapEntityToDto(any())).willReturn(DtoDeliveryAddressType.REGISTERED)

            val expected = with(entityRequest) {
                buildAnonymousElectorDocumentSummaryDto(
                    certificateNumber = certificateNumber,
                    electoralRollNumber = electoralRollNumber,
                    gssCode = gssCode,
                    certificateLanguage = DtoCertificateLanguage.EN,
                    supportingInformationFormat = DtoSupportingInformationFormat.STANDARD,
                    deliveryAddressType = DtoDeliveryAddressType.REGISTERED,
                    elector = with(contactDetails!!) {
                        buildAnonymousElectorDto(
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
                            }
                        )
                    },
                    photoLocation = photoLocationArn,
                    issueDate = issueDate,
                    status = DtoAnonymousElectorDocumentStatus.PRINTED,
                    userId = userId,
                    dateTime = requestDateTime,
                )
            }

            // When
            val actual = mapper.mapToAnonymousElectorDocumentSummaryDto(entityRequest)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(certificateLanguageMapper).mapEntityToDto(entityRequest.certificateLanguage)
            verify(supportingInformationFormatMapper).mapEntityToDto(entityRequest.supportingInformationFormat!!)
            verify(deliveryAddressTypeMapper).mapEntityToDto(REGISTERED)
            verifyNoMoreInteractions(certificateLanguageMapper, supportingInformationFormatMapper, deliveryAddressTypeMapper)
            verifyNoInteractions(instantMapper)
        }
    }

    @Nested
    inner class MapFromContactDetailsToElectorDto {

        @Test
        fun `should map AedContactDetails entity to AnonymousElectorDto for all names present`() {
            // Given
            val aedContactDetailsEntity =
                buildAedContactDetails(firstName = "Joe", middleNames = "John", surname = "Bloggs")
            val expected = with(aedContactDetailsEntity) {
                buildAnonymousElectorDto(
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
                )
            }

            // When
            val actual = mapper.mapFromContactDetailsToElectorDto(aedContactDetailsEntity)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
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
