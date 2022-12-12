package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat.EASY_READ
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat.EASY_MINUS_READ
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.toElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage as CertificateLanguageModel

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

    @Spy
    private val clock: Clock = FIXED_CLOCK

    @ParameterizedTest
    @CsvSource(value = ["EN, EN", "CY, CY"])
    fun `should map send application to print message to print request`(
        certificateLanguageModel: CertificateLanguageModel,
        certificateLanguageEntity: CertificateLanguageEntity
    ) {
        // Given
        val ero = buildEroDto()
        val supportingInformationFormatModelEnum = EASY_MINUS_READ
        val supportingInformationFormatEntityEnum = EASY_READ
        val message = buildSendApplicationToPrintMessage(
            certificateLanguage = certificateLanguageModel,
            supportingInformationFormat = supportingInformationFormatModelEnum
        )
        val requestId = aValidRequestId()
        given(idFactory.requestId()).willReturn(requestId)
        given(supportingInformationFormatMapper.toPrintRequestEntityEnum(any()))
            .willReturn(supportingInformationFormatEntityEnum)
        val expectedEnglishEroContactDetails = ero.englishContactDetails.toElectoralRegistrationOffice()
        val expectedWelshEroContactDetails = ero.welshContactDetails!!.toElectoralRegistrationOffice()
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
                        deliveryMethod = DeliveryMethod.DELIVERY,
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

        // When
        val actual = mapper.toPrintRequest(message, ero)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)
        verify(idFactory).requestId()
        verify(supportingInformationFormatMapper).toPrintRequestEntityEnum(supportingInformationFormatModelEnum)
    }
}
