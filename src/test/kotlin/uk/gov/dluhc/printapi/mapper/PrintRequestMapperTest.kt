package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildIssuerDto
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

    private lateinit var mapper: PrintRequestMapperImpl

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var instantMapper: InstantMapper

    @BeforeEach
    fun setup() {
        mapper = PrintRequestMapperImpl()
        ReflectionTestUtils.setField(mapper, "idFactory", idFactory)
        ReflectionTestUtils.setField(mapper, "clock", FIXED_CLOCK)
        ReflectionTestUtils.setField(mapper, "instantMapper", instantMapper)
    }

    @ParameterizedTest
    @CsvSource(value = ["EN, EN", "CY, CY"])
    fun `should map send application to print message to print details`(
        certificateLanguageModel: CertificateLanguageModel,
        certificateLanguageEntity: CertificateLanguageEntity
    ) {
        // Given
        val issuer = buildIssuerDto()
        val message = buildSendApplicationToPrintMessage(certificateLanguage = certificateLanguageModel)
        val requestId = aValidRequestId()
        given(idFactory.requestId()).willReturn(requestId)
        val expectedEnglishEroContactDetails = ElectoralRegistrationOffice(
            name = "Gwynedd Council Elections",
            phoneNumber = "01766 771000",
            website = "https://www.gwynedd.llyw.cymru/en/Council/Contact-us/Contact-us.aspx",
            emailAddress = "TrethCyngor@gwynedd.llyw.cymru",
            address = Address(
                property = "Gwynedd Council Headquarters",
                street = "Shirehall Street",
                town = "Caernarfon",
                area = "Gwynedd",
                postcode = "LL55 1SH",
            )
        )
        val expectedWelshEroContactDetails = ElectoralRegistrationOffice(
            name = "Etholiadau Cyngor Gwynedd",
            phoneNumber = "01766 771000",
            website = "https://www.gwynedd.llyw.cymru/cy/Cyngor/Cysylltu-%c3%a2-ni/Cysylltu-%c3%a2-ni.aspx",
            emailAddress = "TrethCyngor@gwynedd.llyw.cymru",
            address = Address(
                property = "Pencadlys Cyngor Gwynedd",
                street = "Stryd y Jêl",
                town = "Caernarfon",
                area = "Gwynedd",
                postcode = "LL55 1SH",
            )
        )
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
                supportingInformationFormat = SupportingInformationFormat.STANDARD,
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
                        deliveryMethod = DeliveryMethod.DELIVERY
                    )
                },
                eroEnglish = expectedEnglishEroContactDetails,
                eroWelsh = if (certificateLanguageModel == CertificateLanguage.EN) null else expectedWelshEroContactDetails,
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
        val actual = mapper.toPrintRequest(message, issuer)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)
        verify(idFactory).requestId()
    }
}
