package uk.gov.dluhc.printapi.rds.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.entity.Delivery
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.time.Instant
import java.time.LocalDate
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage as CertificateLanguageModel
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeModel

@ExtendWith(MockitoExtension::class)
class CertificateMapperTest {

    companion object {
        private val FIXED_TIME = Instant.parse("2022-10-18T11:22:32.123Z")
    }

    @InjectMocks
    private lateinit var mapper: CertificateMapperImpl

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var printRequestMapper: PrintRequestMapper

    @Mock
    private lateinit var idFactory: IdFactory

    @Test
    fun `should map send application to print message to print details`() {
        // Given
        val ero = buildEroManagementApiEroDto()
        val localAuthority = ero.localAuthorities[0]
        val message = buildSendApplicationToPrintMessage(certificateLanguage = CertificateLanguageModel.EN)
        val requestId = aValidRequestId()
        val vacNumber = aValidVacNumber()
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(SourceTypeEntity.VOTER_CARD)
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

        val printRequest = with(message) {
            PrintRequest(
                requestDateTime = requestDateTime.toInstant(),
                requestId = requestId,
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = CertificateLanguageEntity.EN,
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
                eroWelsh = null,
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
        given(printRequestMapper.toPrintRequest(any(), any())).willReturn(printRequest)

        val expected = with(message) {
            Certificate(
                id = null,
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                sourceType = SourceTypeEntity.VOTER_CARD,
                vacNumber = vacNumber,
                applicationReceivedDateTime = applicationReceivedDateTime,
                gssCode = gssCode,
                issuingAuthority = localAuthority.name,
                issueDate = LocalDate.now(),
                printRequests = mutableListOf(printRequest)
            )
        }

        // When
        val actual = mapper.toCertificate(message, ero, localAuthority.name)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id").isEqualTo(expected)
        assertThat(actual.status).isNull()
        assertThat(actual.id).isNull()
        verify(sourceTypeMapper).toSourceTypeEntity(SourceTypeModel.VOTER_MINUS_CARD)
        verify(idFactory).vacNumber()
        verify(printRequestMapper).toPrintRequest(message, ero)
    }
}
