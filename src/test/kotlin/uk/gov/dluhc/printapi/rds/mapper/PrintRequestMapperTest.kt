package uk.gov.dluhc.printapi.rds.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.Delivery
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto
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
    private lateinit var rdsElectoralRegistrationOfficeMapper: RdsElectoralRegistrationOfficeMapper

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
        ReflectionTestUtils.setField(mapper, "rdsElectoralRegistrationOfficeMapper", rdsElectoralRegistrationOfficeMapper)
    }

    @ParameterizedTest
    @CsvSource(value = ["EN, EN", "CY, CY"])
    fun `should map send application to print message to print details`(
        certificateLanguageModel: CertificateLanguageModel,
        certificateLanguageEntity: CertificateLanguageEntity
    ) {
        // Given
        val ero = buildEroManagementApiEroDto()
        val message = buildSendApplicationToPrintMessage(certificateLanguage = certificateLanguageModel)
        val requestId = aValidRequestId()
        given(idFactory.requestId()).willReturn(requestId)
        val electoralRegistrationOffice = ElectoralRegistrationOffice(
            name = "Croydon London Borough Council",
            phoneNumber = "",
            emailAddress = "",
            website = "",
            address = Address(
                street = "",
                postcode = ""
            )
        )
        given(rdsElectoralRegistrationOfficeMapper.map(any())).willReturn(electoralRegistrationOffice)
        val expectedRequestDateTime = message.requestDateTime.toInstant()
        given(instantMapper.toInstant(any())).willReturn(expectedRequestDateTime)
        val expected = with(message) {
            PrintRequest(
                requestDateTime = expectedRequestDateTime,
                requestId = requestId,
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = certificateLanguageEntity,
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
                eroEnglish = electoralRegistrationOffice,
                eroWelsh = if (certificateLanguageModel == CertificateLanguage.EN) null else electoralRegistrationOffice,
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
        val expectedElectoralRegistrationOfficeMapperInvocations = if (certificateLanguageModel == CertificateLanguage.EN) 1 else 2
        verify(rdsElectoralRegistrationOfficeMapper, times(expectedElectoralRegistrationOfficeMapperInvocations)).map(ero)
    }
}
