package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat.EASY_READ
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType.REGISTERED
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat.EASY_MINUS_READ
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.toElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildCertificateDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage as CertificateLanguageModel

private const val ELECTORAL_REGISTRATION_OFFICER_EN = "Electoral Registration Officer"
private const val ELECTORAL_REGISTRATION_OFFICER_CY = "Swyddog Cofrestru Etholiadol"

@ExtendWith(MockitoExtension::class)
class PrintRequestMapperTest {

    companion object {
        private val FIXED_TIME = Instant.parse("2022-10-18T11:22:32.123Z")
        private val FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
    }

    @InjectMocks
    private lateinit var mapper: PrintRequestMapperImpl

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: SupportingInformationFormatMapper

    @Mock
    private lateinit var deliveryAddressTypeMapper: DeliveryAddressTypeMapper

    @Spy
    private val clock: Clock = FIXED_CLOCK

    @Mock
    private lateinit var electoralRegistrationOfficeMapper: ElectoralRegistrationOfficeMapper

    @ParameterizedTest
    @CsvSource(value = ["EN, EN", "CY, CY"])
    fun `should map send application to print message to print request given existing Welsh address details`(
        certificateLanguageModel: CertificateLanguageModel,
        certificateLanguageEntity: CertificateLanguageEntity
    ) {
        // Given
        val ero = buildEroDto()
        val supportingInformationFormatModelEnum = EASY_MINUS_READ
        val supportingInformationFormatEntityEnum = EASY_READ
        val deliveryAddressTypeModelEnum = REGISTERED
        val message = buildSendApplicationToPrintMessage(
            certificateLanguage = certificateLanguageModel,
            supportingInformationFormat = supportingInformationFormatModelEnum,
            delivery = buildCertificateDelivery(
                deliveryAddressType = deliveryAddressTypeModelEnum
            )
        )
        val requestId = aValidRequestId()
        given(idFactory.requestId()).willReturn(requestId)
        given(supportingInformationFormatMapper.toPrintRequestEntityEnum(any()))
            .willReturn(supportingInformationFormatEntityEnum)
        val expectedEnglishEroContactDetails =
            ero.englishContactDetails.toElectoralRegistrationOffice(ELECTORAL_REGISTRATION_OFFICER_EN)
        val expectedWelshEroContactDetails =
            ero.welshContactDetails!!.toElectoralRegistrationOffice(ELECTORAL_REGISTRATION_OFFICER_CY)
        given(electoralRegistrationOfficeMapper.toElectoralRegistrationOffice(any(), eq(CertificateLanguageModel.EN)))
            .willReturn(expectedEnglishEroContactDetails)
        given(electoralRegistrationOfficeMapper.toElectoralRegistrationOffice(any(), eq(CertificateLanguageModel.CY)))
            .willReturn(expectedWelshEroContactDetails)
        val expectedRequestDateTime = message.requestDateTime.toInstant()
        given(instantMapper.toInstant(any())).willReturn(expectedRequestDateTime)
        val expected = with(message) {
            PrintRequest(
                requestDateTime = expectedRequestDateTime,
                requestId = requestId,
                vacVersion = "A",
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = certificateLanguageEntity,
                supportingInformationFormat = supportingInformationFormatEntityEnum,
                photoLocationArn = photoLocation,
                delivery = with(delivery) {
                    Delivery(
                        addressee = addressee,
                        address = with(address) {
                            Address(
                                street = street,
                                postcode = postcode,
                                property = property,
                                locality = locality,
                                town = town,
                                area = area,
                                uprn = uprn
                            )
                        },
                        deliveryClass = DeliveryClass.STANDARD,
                        deliveryAddressType = DeliveryAddressType.REGISTERED,
                        addressFormat = AddressFormat.UK,
                    )
                },
                eroEnglish = expectedEnglishEroContactDetails,
                eroWelsh = expectedWelshEroContactDetails,
                statusHistory = mutableListOf(
                    PrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        dateCreated = FIXED_TIME,
                        eventDateTime = FIXED_TIME
                    )
                ),
                userId = userId
            )
        }
        given(deliveryAddressTypeMapper.toDeliveryAddressTypeEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        // When
        val actual = mapper.toPrintRequest(message, ero)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)
        verify(idFactory).requestId()
        verify(supportingInformationFormatMapper).toPrintRequestEntityEnum(supportingInformationFormatModelEnum)
        verify(deliveryAddressTypeMapper).toDeliveryAddressTypeEntity(deliveryAddressTypeModelEnum)
        verify(electoralRegistrationOfficeMapper)
            .toElectoralRegistrationOffice(ero.englishContactDetails, CertificateLanguageModel.EN)
        verify(electoralRegistrationOfficeMapper)
            .toElectoralRegistrationOffice(ero.welshContactDetails, CertificateLanguageModel.CY)
    }

    @Test
    fun `should map send application to print message to print request given no Welsh address details and certificate language is English`() {
        // Given
        val ero = buildEroDto(welshContactDetails = null)
        val supportingInformationFormatModelEnum = EASY_MINUS_READ
        val supportingInformationFormatEntityEnum = EASY_READ
        val deliveryAddressTypeModelEnum = REGISTERED
        val message = buildSendApplicationToPrintMessage(
            certificateLanguage = CertificateLanguageModel.EN,
            supportingInformationFormat = supportingInformationFormatModelEnum,
            delivery = buildCertificateDelivery(
                deliveryAddressType = deliveryAddressTypeModelEnum
            )
        )
        val requestId = aValidRequestId()
        given(idFactory.requestId()).willReturn(requestId)
        given(supportingInformationFormatMapper.toPrintRequestEntityEnum(any()))
            .willReturn(supportingInformationFormatEntityEnum)
        val expectedEnglishEroContactDetails =
            ero.englishContactDetails.toElectoralRegistrationOffice(ELECTORAL_REGISTRATION_OFFICER_EN)
        val expectedWelshEroContactDetails = null
        given(electoralRegistrationOfficeMapper.toElectoralRegistrationOffice(any(), eq(CertificateLanguageModel.EN)))
            .willReturn(expectedEnglishEroContactDetails)
        given(
            electoralRegistrationOfficeMapper.toElectoralRegistrationOffice(
                anyOrNull(),
                eq(CertificateLanguageModel.CY)
            )
        )
            .willReturn(expectedWelshEroContactDetails)
        val expectedRequestDateTime = message.requestDateTime.toInstant()
        given(instantMapper.toInstant(any())).willReturn(expectedRequestDateTime)
        val expected = with(message) {
            PrintRequest(
                requestDateTime = expectedRequestDateTime,
                requestId = requestId,
                vacVersion = "A",
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = CertificateLanguageEntity.EN,
                supportingInformationFormat = supportingInformationFormatEntityEnum,
                photoLocationArn = photoLocation,
                delivery = with(delivery) {
                    Delivery(
                        addressee = addressee,
                        address = with(address) {
                            Address(
                                street = street,
                                postcode = postcode,
                                property = property,
                                locality = locality,
                                town = town,
                                area = area,
                                uprn = uprn
                            )
                        },
                        deliveryClass = DeliveryClass.STANDARD,
                        deliveryAddressType = DeliveryAddressType.REGISTERED,
                        addressFormat = AddressFormat.UK,
                    )
                },
                eroEnglish = expectedEnglishEroContactDetails,
                eroWelsh = expectedWelshEroContactDetails,
                statusHistory = mutableListOf(
                    PrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        dateCreated = FIXED_TIME,
                        eventDateTime = FIXED_TIME
                    )
                ),
                userId = userId
            )
        }
        given(deliveryAddressTypeMapper.toDeliveryAddressTypeEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        // When
        val actual = mapper.toPrintRequest(message, ero)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)
        verify(idFactory).requestId()
        verify(supportingInformationFormatMapper).toPrintRequestEntityEnum(supportingInformationFormatModelEnum)
        verify(deliveryAddressTypeMapper).toDeliveryAddressTypeEntity(deliveryAddressTypeModelEnum)
        verify(electoralRegistrationOfficeMapper)
            .toElectoralRegistrationOffice(ero.englishContactDetails, CertificateLanguageModel.EN)
        verify(electoralRegistrationOfficeMapper)
            .toElectoralRegistrationOffice(
                ero.welshContactDetails, CertificateLanguageModel.CY
            )
    }

    @Test
    fun `should map send application to print message to print request given no Welsh address details and certificate language is Welsh`() {
        // Given
        val ero = buildEroDto(welshContactDetails = null)
        val supportingInformationFormatModelEnum = EASY_MINUS_READ
        val supportingInformationFormatEntityEnum = EASY_READ
        val deliveryAddressTypeModelEnum = REGISTERED
        val message = buildSendApplicationToPrintMessage(
            certificateLanguage = CertificateLanguageModel.CY,
            supportingInformationFormat = supportingInformationFormatModelEnum,
            delivery = buildCertificateDelivery(
                deliveryAddressType = deliveryAddressTypeModelEnum
            )
        )
        val requestId = aValidRequestId()
        given(idFactory.requestId()).willReturn(requestId)
        given(supportingInformationFormatMapper.toPrintRequestEntityEnum(any()))
            .willReturn(supportingInformationFormatEntityEnum)
        val expectedEnglishEroContactDetails =
            ero.englishContactDetails.toElectoralRegistrationOffice(ELECTORAL_REGISTRATION_OFFICER_EN)
        val expectedWelshEroContactDetails =
            ero.englishContactDetails.toElectoralRegistrationOffice(ELECTORAL_REGISTRATION_OFFICER_EN)
        given(electoralRegistrationOfficeMapper.toElectoralRegistrationOffice(any(), eq(CertificateLanguageModel.EN)))
            .willReturn(expectedEnglishEroContactDetails)
        val expectedRequestDateTime = message.requestDateTime.toInstant()
        given(instantMapper.toInstant(any())).willReturn(expectedRequestDateTime)
        val expected = with(message) {
            PrintRequest(
                requestDateTime = expectedRequestDateTime,
                requestId = requestId,
                vacVersion = "A",
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = CertificateLanguageEntity.CY,
                supportingInformationFormat = supportingInformationFormatEntityEnum,
                photoLocationArn = photoLocation,
                delivery = with(delivery) {
                    Delivery(
                        addressee = addressee,
                        address = with(address) {
                            Address(
                                street = street,
                                postcode = postcode,
                                property = property,
                                locality = locality,
                                town = town,
                                area = area,
                                uprn = uprn
                            )
                        },
                        deliveryClass = DeliveryClass.STANDARD,
                        deliveryAddressType = DeliveryAddressType.REGISTERED,
                        addressFormat = AddressFormat.UK,
                    )
                },
                eroEnglish = expectedEnglishEroContactDetails,
                eroWelsh = expectedWelshEroContactDetails,
                statusHistory = mutableListOf(
                    PrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        dateCreated = FIXED_TIME,
                        eventDateTime = FIXED_TIME
                    )
                ),
                userId = userId
            )
        }
        given(deliveryAddressTypeMapper.toDeliveryAddressTypeEntity(any())).willReturn(DeliveryAddressType.REGISTERED)

        // When
        val actual = mapper.toPrintRequest(message, ero)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)
        verify(idFactory).requestId()
        verify(supportingInformationFormatMapper).toPrintRequestEntityEnum(supportingInformationFormatModelEnum)
        verify(deliveryAddressTypeMapper).toDeliveryAddressTypeEntity(deliveryAddressTypeModelEnum)
        verify(electoralRegistrationOfficeMapper, times(2))
            .toElectoralRegistrationOffice(ero.englishContactDetails, CertificateLanguageModel.EN)
        verify(electoralRegistrationOfficeMapper, never())
            .toElectoralRegistrationOffice(ero.welshContactDetails, CertificateLanguageModel.CY)
    }
}
