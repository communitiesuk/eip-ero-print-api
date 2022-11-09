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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.util.ReflectionTestUtils
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.CertificateDelivery
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage as CertificateLanguageModel
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeModel

@ExtendWith(MockitoExtension::class)
class PrintDetailsMapperTest {

    companion object {
        private val FIXED_TIME = Instant.parse("2022-10-18T11:22:32.123Z")
        private val FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
    }

    private lateinit var mapper: PrintDetailsMapperImpl

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var electoralRegistrationOfficeMapper: ElectoralRegistrationOfficeMapper

    @Mock
    private lateinit var idFactory: IdFactory

    @BeforeEach
    fun setup() {
        mapper = PrintDetailsMapperImpl()
        ReflectionTestUtils.setField(mapper, "sourceTypeMapper", sourceTypeMapper)
        ReflectionTestUtils.setField(mapper, "idFactory", idFactory)
        ReflectionTestUtils.setField(mapper, "clock", FIXED_CLOCK)
        ReflectionTestUtils.setField(mapper, "electoralRegistrationOfficeMapper", electoralRegistrationOfficeMapper)
    }

    @ParameterizedTest
    @CsvSource(value = ["EN, EN", "CY, CY"])
    fun `should map send application to print message to print details`(
        certificateLanguageModel: CertificateLanguageModel,
        certificateLanguageEntity: CertificateLanguageEntity
    ) {
        // Given
        val ero = buildEroManagementApiEroDto()
        val localAuthority = ero.localAuthorities[0]
        val message = buildSendApplicationToPrintMessage(certificateLanguage = certificateLanguageModel)
        val requestId = aValidRequestId()
        val vacNumber = aValidVacNumber()
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(SourceTypeEntity.VOTER_CARD)
        given(idFactory.requestId()).willReturn(requestId)
        given(idFactory.vacNumber()).willReturn(vacNumber)
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
        given(electoralRegistrationOfficeMapper.map(any())).willReturn(electoralRegistrationOffice)

        val expected = with(message) {
            PrintDetails(
                id = UUID.randomUUID(),
                requestId = requestId,
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                sourceType = SourceTypeEntity.VOTER_CARD,
                vacNumber = vacNumber,
                requestDateTime = requestDateTime,
                applicationReceivedDateTime = applicationReceivedDateTime,
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = certificateLanguageEntity,
                photoLocation = photoLocation,
                delivery = with(delivery) {
                    CertificateDelivery(
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
                gssCode = gssCode,
                issuingAuthority = localAuthority.name,
                issueDate = LocalDate.now(),
                eroEnglish = electoralRegistrationOffice,
                eroWelsh = if (certificateLanguageModel == CertificateLanguage.EN) null else electoralRegistrationOffice,
                printRequestStatuses = mutableListOf(
                    PrintRequestStatus(
                        Status.PENDING_ASSIGNMENT_TO_BATCH,
                        dateCreated = FIXED_TIME.atOffset(ZoneOffset.UTC),
                        eventDateTime = FIXED_TIME.atOffset(ZoneOffset.UTC)
                    )
                ),
                userId = userId
            )
        }
        // When

        val actual = mapper.toPrintDetails(message, ero, localAuthority.name)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)
        assertThat(actual.status).isEqualTo(Status.PENDING_ASSIGNMENT_TO_BATCH)
        assertThat(actual.id).isNotNull
        verify(sourceTypeMapper).toSourceTypeEntity(SourceTypeModel.VOTER_MINUS_CARD)
        verify(idFactory).requestId()
        verify(idFactory).vacNumber()
        val expectedElectoralRegistrationOfficeMapperInvocations = if (certificateLanguageModel == CertificateLanguage.EN) 1 else 2
        verify(electoralRegistrationOfficeMapper, times(expectedElectoralRegistrationOfficeMapperInvocations)).map(ero)
    }
}
