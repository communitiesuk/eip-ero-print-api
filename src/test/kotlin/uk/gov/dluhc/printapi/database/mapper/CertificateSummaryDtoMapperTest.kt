package uk.gov.dluhc.printapi.database.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.VALIDATED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto
import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto
import uk.gov.dluhc.printapi.dto.PrintRequestSummaryDto
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapperImpl
import uk.gov.dluhc.printapi.testsupport.testdata.aDifferentValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aDifferentValidDeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildPrintRequestSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.time.Instant
import java.time.Instant.now
import java.time.temporal.ChronoUnit.MINUTES

internal class CertificateSummaryDtoMapperTest {

    private val mapper = CertificateSummaryDtoMapper()
    private val deliveryAddressTypeMapper: DeliveryAddressTypeMapper = DeliveryAddressTypeMapperImpl()

    @Test
    fun `should map from Certificate to CertificatePrintRequestSummary given single print request with one status`() {
        // Given
        val printRequestStatus = buildPrintRequestStatus()
        val printRequest = buildPrintRequest(printRequestStatuses = listOf(printRequestStatus))
        val certificate = buildCertificate(printRequests = listOf(printRequest))
        val expected = with(certificate) {
            buildCertificateSummaryDto(
                sourceReference = sourceReference!!,
                applicationReference = applicationReference!!,
                vacNumber = vacNumber!!,
                firstName = printRequest.firstName!!,
                middleNames = printRequest.middleNames,
                surname = printRequest.surname!!,
                printRequests = listOf(
                    buildPrintRequestSummaryDto(
                        userId = printRequest.userId!!,
                        status = PrintRequestStatusDto.valueOf(printRequestStatus.status!!.name),
                        eventDateTime = printRequestStatus.eventDateTime!!,
                        message = printRequestStatus.message,
                        deliveryAddressType = deliveryAddressTypeMapper.mapEntityToDto(printRequest.delivery!!.deliveryAddressType)
                    )
                )
            )
        }

        // When
        val actual = mapper.certificateToCertificatePrintRequestSummaryDto(certificate)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should map from Certificate to CertificatePrintRequestSummary given one print request with multiple status`() {
        // Given
        val expectedMessage = "The certificate has been dispatched"
        val expectedTime = now()
        val printRequestStatuses = listOf(
            buildPrintRequestStatus(status = PENDING_ASSIGNMENT_TO_BATCH, eventDateTime = expectedTime.minusSeconds(10)),
            buildPrintRequestStatus(status = ASSIGNED_TO_BATCH, eventDateTime = expectedTime.minusSeconds(9)),
            buildPrintRequestStatus(status = SENT_TO_PRINT_PROVIDER, eventDateTime = expectedTime.minusSeconds(8)),
            buildPrintRequestStatus(status = RECEIVED_BY_PRINT_PROVIDER, eventDateTime = expectedTime.minusSeconds(7)),
            buildPrintRequestStatus(status = VALIDATED_BY_PRINT_PROVIDER, eventDateTime = expectedTime.minusSeconds(6)),
            buildPrintRequestStatus(status = IN_PRODUCTION, eventDateTime = expectedTime.minusSeconds(5)),
            buildPrintRequestStatus(status = DISPATCHED, eventDateTime = expectedTime, message = expectedMessage),
        )
        val printRequest = buildPrintRequest(printRequestStatuses = printRequestStatuses)
        val certificate = buildCertificate(printRequests = listOf(printRequest))
        val expected = with(certificate) {
            buildCertificateSummaryDto(
                sourceReference = sourceReference!!,
                applicationReference = applicationReference!!,
                vacNumber = vacNumber!!,
                firstName = printRequest.firstName!!,
                middleNames = printRequest.middleNames,
                surname = printRequest.surname!!,
                printRequests = listOf(
                    buildPrintRequestSummaryDto(
                        userId = printRequest.userId!!,
                        status = PrintRequestStatusDto.DISPATCHED,
                        eventDateTime = expectedTime,
                        message = expectedMessage,
                        deliveryAddressType = deliveryAddressTypeMapper.mapEntityToDto(printRequest.delivery!!.deliveryAddressType)
                    )
                )
            )
        }

        // When
        val actual = mapper.certificateToCertificatePrintRequestSummaryDto(certificate)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should map from Certificate to CertificatePrintRequestSummary given multiple print requests`() {
        // Given
        val expectedStatus1 = aValidCertificateStatus()
        val expectedStatus2 = aDifferentValidCertificateStatus()
        val expectedDateTime1 = now().minus(1, MINUTES)
        val expectedDateTime2 = now()
        val expectedUserId1 = aValidUserId()
        val expectedUserId2 = aValidUserId()
        val expectedMessage1 = null
        val expectedMessage2 = "Successfully dispatched by Royal Mail"
        val deliveryAddressType1 = aValidDeliveryAddressType()
        val deliveryAddressType2 = aDifferentValidDeliveryAddressType()
        val firstName1 = aValidFirstName()
        val firstName2 = aValidFirstName()
        val surname1 = aValidSurname()
        val surname2 = aValidSurname()
        val printRequestStatus1 = buildPrintRequestStatus(status = expectedStatus1, eventDateTime = expectedDateTime1, message = expectedMessage1)
        val printRequestStatus2 = buildPrintRequestStatus(status = expectedStatus2, eventDateTime = expectedDateTime2, message = expectedMessage2)
        val printRequest1 = buildPrintRequest(
            userId = expectedUserId1,
            printRequestStatuses = listOf(printRequestStatus1),
            firstName = firstName1,
            surname = surname1,
            delivery = buildDelivery(deliveryAddressType = deliveryAddressType1)
        )
        val printRequest2 = buildPrintRequest(
            userId = expectedUserId2,
            printRequestStatuses = listOf(printRequestStatus2),
            firstName = firstName2,
            surname = surname2,
            delivery = buildDelivery(deliveryAddressType = deliveryAddressType2)
        )
        val certificate = buildCertificate(printRequests = listOf(printRequest1, printRequest2))
        val expected = with(certificate) {
            CertificateSummaryDto(
                sourceReference = sourceReference!!,
                applicationReference = applicationReference!!,
                vacNumber = vacNumber!!,
                firstName = printRequest2.firstName!!,
                middleNames = printRequest2.middleNames,
                surname = printRequest2.surname!!,
                printRequests = listOf(
                    PrintRequestSummaryDto(
                        status = PrintRequestStatusDto.valueOf(expectedStatus2.name),
                        dateTime = expectedDateTime2,
                        userId = expectedUserId2,
                        message = expectedMessage2,
                        deliveryAddressType = deliveryAddressTypeMapper.mapEntityToDto(deliveryAddressType2)
                    ),
                    PrintRequestSummaryDto(
                        status = PrintRequestStatusDto.valueOf(expectedStatus1.name),
                        dateTime = expectedDateTime1,
                        userId = expectedUserId1,
                        message = expectedMessage1,
                        deliveryAddressType = deliveryAddressTypeMapper.mapEntityToDto(deliveryAddressType1)
                    ),
                )
            )
        }

        // When
        val actual = mapper.certificateToCertificatePrintRequestSummaryDto(certificate)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    private fun printRequestStatus(status: Status, eventDateTime: Instant, message: String?) =
        buildPrintRequestStatus(status = status, eventDateTime = eventDateTime, message = message)
}
