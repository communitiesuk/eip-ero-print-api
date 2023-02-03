package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aWelshEroContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.toElectoralRegistrationOffice
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
    private lateinit var instantMapper: InstantMapper

    @Mock
    private lateinit var printRequestMapper: PrintRequestMapper

    @Mock
    private lateinit var idFactory: IdFactory

    @Test
    fun `should map send application to print message for an English certificate to print details`() {
        // Given
        val ero = buildEroDto(
            welshContactDetails = null
        )
        val message = buildSendApplicationToPrintMessage(certificateLanguage = CertificateLanguageModel.EN)
        val requestId = aValidRequestId()
        val vacNumber = aValidVacNumber()
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(SourceTypeEntity.VOTER_CARD)
        given(idFactory.vacNumber()).willReturn(vacNumber)
        given(instantMapper.toInstant(any())).willReturn(message.applicationReceivedDateTime.toInstant())

        val englishEro = ero.englishContactDetails.toElectoralRegistrationOffice(ero.englishContactDetails.name)

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
                        deliveryAddressType = DeliveryAddressType.REGISTERED,
                        addressFormat = AddressFormat.UK,
                    )
                },
                eroEnglish = englishEro,
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
                applicationReceivedDateTime = applicationReceivedDateTime.toInstant(),
                gssCode = gssCode,
                issuingAuthority = ero.englishContactDetails.name,
                issuingAuthorityCy = null,
                issueDate = LocalDate.now(),
                printRequests = mutableListOf(printRequest),
                status = Status.PENDING_ASSIGNMENT_TO_BATCH,
            )
        }

        // When
        val actual = mapper.toCertificate(message, ero)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(sourceTypeMapper).toSourceTypeEntity(SourceTypeModel.VOTER_MINUS_CARD)
        verify(idFactory).vacNumber()
        verify(printRequestMapper).toPrintRequest(message, ero)
        verify(instantMapper).toInstant(message.applicationReceivedDateTime)
    }

    @Test
    fun `should map send application to print message for a Welsh certificate to print details`() {
        // Given
        val ero = buildEroDto(
            welshContactDetails = aWelshEroContactDetails()
        )
        val message = buildSendApplicationToPrintMessage(certificateLanguage = CertificateLanguageModel.CY)
        val requestId = aValidRequestId()
        val vacNumber = aValidVacNumber()
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(SourceTypeEntity.VOTER_CARD)
        given(idFactory.vacNumber()).willReturn(vacNumber)
        given(instantMapper.toInstant(any())).willReturn(message.applicationReceivedDateTime.toInstant())

        val englishEro = ero.englishContactDetails.toElectoralRegistrationOffice(ero.englishContactDetails.name)
        val welshEro = ero.welshContactDetails!!.toElectoralRegistrationOffice(ero.welshContactDetails!!.name)

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
                        deliveryAddressType = DeliveryAddressType.REGISTERED,
                        addressFormat = AddressFormat.UK,
                    )
                },
                eroEnglish = englishEro,
                eroWelsh = welshEro,
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
                applicationReceivedDateTime = applicationReceivedDateTime.toInstant(),
                gssCode = gssCode,
                issuingAuthority = ero.englishContactDetails.name,
                issuingAuthorityCy = ero.welshContactDetails!!.name,
                issueDate = LocalDate.now(),
                printRequests = mutableListOf(printRequest),
                status = Status.PENDING_ASSIGNMENT_TO_BATCH,
            )
        }

        // When
        val actual = mapper.toCertificate(message, ero)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(sourceTypeMapper).toSourceTypeEntity(SourceTypeModel.VOTER_MINUS_CARD)
        verify(idFactory).vacNumber()
        verify(printRequestMapper).toPrintRequest(message, ero)
        verify(instantMapper).toInstant(message.applicationReceivedDateTime)
    }
}
