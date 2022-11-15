package uk.gov.dluhc.printapi.rds.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.entity.Delivery
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressPostcode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressStreet
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReceivedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryMethod
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssuingAuthority
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestStatusEventDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSuggestedExpiryDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacVersion
import uk.gov.dluhc.printapi.testsupport.testdata.aValidWebsite
import uk.gov.dluhc.printapi.testsupport.testdata.rds.certificateBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.printRequestBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.time.OffsetDateTime
import java.util.function.BiPredicate

internal class CertificateRepositoryTest : IntegrationTest() {

    @Test
    fun `should return Certificate given persisted Certificate with all required properties`() {
        // Given
        val certificate = Certificate(
            vacNumber = aValidVacNumber(),
            sourceType = aValidSourceType(),
            sourceReference = aValidSourceReference(),
            applicationReference = aValidApplicationReference(),
            applicationReceivedDateTime = aValidApplicationReceivedDateTime(),
            issuingAuthority = aValidIssuingAuthority(),
            issueDate = aValidIssueDate(),
            suggestedExpiryDate = aValidSuggestedExpiryDate(),
            gssCode = aGssCode(),
            status = null
        )
        val deliveryAddress = Address(
            street = aValidAddressStreet(),
            postcode = aValidAddressPostcode()
        )
        val delivery = Delivery(
            addressee = aValidDeliveryName(),
            address = deliveryAddress,
            deliveryClass = aValidDeliveryClass(),
            deliveryMethod = aValidDeliveryMethod()
        )
        val eroEnglish = ElectoralRegistrationOffice(
            address = Address(
                street = aValidAddressStreet(),
                postcode = aValidAddressPostcode()
            ),
            name = aValidEroName(),
            phoneNumber = aValidPhoneNumber(),
            emailAddress = aValidEmailAddress(),
            website = aValidWebsite()
        )
        val printRequest = PrintRequest(
            requestId = aValidRequestId(),
            vacVersion = aValidVacVersion(),
            requestDateTime = aValidRequestDateTime(),
            firstName = aValidFirstName(),
            surname = aValidSurname(),
            certificateLanguage = aValidCertificateLanguage(),
            certificateFormat = aValidCertificateFormat(),
            photoLocationArn = aPhotoArn(),
            delivery = delivery,
            eroEnglish = eroEnglish,
            eroWelsh = null,
            userId = aValidUserId()
        )
        val printRequestStatus = PrintRequestStatus(
            status = aValidCertificateStatus(),
            eventDateTime = aValidPrintRequestStatusEventDateTime(),
        )
        printRequest.addPrintRequestStatus(printRequestStatus)
        certificate.addPrintRequest(printRequest)
        val expected = certificateRepository.save(certificate)

        // When
        val actual = certificateRepository.findById(expected.id!!)

        // Then
        assertThat(actual).isPresent
        assertCertificateRecursiveEqual(actual.get(), expected)
        assertThat(actual.get().status).isEqualTo(printRequestStatus.status)
    }

    @Test
    fun `should get by requestId given one exists`() {
        // Given
        val requestId = aValidRequestId()
        val certificate = certificateBuilder(printRequests = listOf(printRequestBuilder(requestId = requestId)))
        val expected = certificateRepository.save(certificate)

        // When
        val actual = certificateRepository.getByPrintRequestsRequestId(requestId)

        // Given
        assertCertificateRecursiveEqual(actual, expected)
    }

    @Test
    fun `should get by status and batchId`() {
        // Given
        val batchId = aValidBatchId()
        val status = aValidCertificateStatus()
        val certificate = certificateBuilder(
            status = status,
            printRequests = listOf(printRequestBuilder(batchId = batchId))
        )
        val expected = certificateRepository.save(certificate)

        // When
        val actual = certificateRepository.findByStatusAndPrintRequestsBatchId(status, batchId)

        // Given
        assertThat(actual).hasSize(1)
        assertThat(actual[0].status).isEqualTo(status)
        assertThat(actual[0].printRequests[0].batchId).isEqualTo(batchId)
        assertCertificateRecursiveEqual(actual[0], expected)
    }

    @Test
    fun `should get by status`() {
        // Given
        val status = aValidCertificateStatus()
        val certificate = certificateBuilder(
            status = status,
        )
        val expected = certificateRepository.save(certificate)

        // When
        val actual = certificateRepository.findByStatus(status)

        // Given
        assertThat(actual).hasSize(1)
        assertThat(actual[0].status).isEqualTo(status)
        assertCertificateRecursiveEqual(actual[0], expected)
    }

    private fun assertCertificateRecursiveEqual(actual: Certificate, expected: Certificate) {
        val offsetEqualToRoundedSeconds: BiPredicate<OffsetDateTime, OffsetDateTime> =
            BiPredicate<OffsetDateTime, OffsetDateTime> { a, b -> withinSecond(a.toEpochSecond(), b.toEpochSecond()) }
        val instantEqualToRoundedSeconds: BiPredicate<Instant, Instant> =
            BiPredicate<Instant, Instant> { a, b -> withinSecond(a.epochSecond, b.epochSecond) }
        assertThat(actual).usingRecursiveComparison()
            .withEqualsForType(offsetEqualToRoundedSeconds, OffsetDateTime::class.java)
            .withEqualsForType(instantEqualToRoundedSeconds, Instant::class.java)
            .isEqualTo(expected)
    }

    fun withinSecond(actual: Long, epochSeconds: Long): Boolean {
        val variance = 1
        val lowerBound: Long = epochSeconds - variance
        val upperBound: Long = epochSeconds + variance
        return actual in lowerBound..upperBound
    }
}
