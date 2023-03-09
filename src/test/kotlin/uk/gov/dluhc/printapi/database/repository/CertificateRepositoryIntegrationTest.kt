package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findDistinctByPrintRequestStatusAndBatchId
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressPostcode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressStreet
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReceivedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryClass
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
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacVersion
import uk.gov.dluhc.printapi.testsupport.testdata.aValidWebsite
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCodeList
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.ChronoUnit.SECONDS

internal class CertificateRepositoryIntegrationTest : IntegrationTest() {

    companion object {
        private val IGNORED_FIELDS = arrayOf(".*dateCreated")
    }

    @Nested
    inner class FindById {
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
                status = null,
                photoLocationArn = aPhotoArn(),
            )
            val deliveryAddress = Address(
                street = aValidAddressStreet(),
                postcode = aValidAddressPostcode()
            )
            val delivery = Delivery(
                addressee = aValidDeliveryName(),
                address = deliveryAddress,
                deliveryClass = aValidDeliveryClass(),
                deliveryAddressType = aValidDeliveryAddressType(),
                addressFormat = aValidAddressFormat(),
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
                supportingInformationFormat = aValidSupportingInformationFormat(),
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
            assertThat(actual).get()
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*IGNORED_FIELDS)
                .isEqualTo(expected)
        }
    }

    @Nested
    inner class GetByPrintRequestsRequestId {
        @Test
        fun `should get by requestId given one exists`() {
            // Given
            val requestId = aValidRequestId()
            val certificate = buildCertificate(printRequests = listOf(buildPrintRequest(requestId = requestId)))
            val expected = certificateRepository.save(certificate)

            // When
            val actual = certificateRepository.getByPrintRequestsRequestId(requestId)

            // Given
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*IGNORED_FIELDS)
                .isEqualTo(expected)
        }

        @Test
        fun `should return null given no certificate by the requestId`() {
            // Given
            val certificate = buildCertificate(printRequests = listOf(buildPrintRequest()))
            certificateRepository.save(certificate)

            // When
            val actual = certificateRepository.getByPrintRequestsRequestId("non-existing-request-id")

            // Given
            assertThat(actual).isNull()
        }
    }

    @Nested
    inner class FindDistinctByPrintRequestStatusAndBatchId {
        @Test
        fun `should get the certificate with matching print request`() {
            // Given
            val batchId = aValidBatchId()
            val status = aValidCertificateStatus()
            val certificate = buildCertificate(
                status = status,
                printRequests = listOf(buildPrintRequest(batchId = batchId))
            )
            val expected = listOf(certificateRepository.save(certificate))

            // When
            val actual = certificateRepository.findDistinctByPrintRequestStatusAndBatchId(status, batchId)

            // Given
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*IGNORED_FIELDS)
                .isEqualTo(expected)
        }

        @Test
        fun `should get the certificate with matching print requests`() {
            // Given
            val batchId = aValidBatchId()
            val status = aValidCertificateStatus()
            val certificate = buildCertificate(
                status = status,
                printRequests = listOf(buildPrintRequest(batchId = batchId), buildPrintRequest(batchId = batchId))
            )
            val expected = listOf(certificateRepository.save(certificate))

            // When
            val actual = certificateRepository.findDistinctByPrintRequestStatusAndBatchId(status, batchId)

            // Given
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*IGNORED_FIELDS)
                .ignoringCollectionOrder()
                .isEqualTo(expected)
        }

        @Test
        fun `should get an empty list as no matching Certificates`() {
            // Given
            val batchId = aValidBatchId()
            val certificate = buildCertificate(
                status = PENDING_ASSIGNMENT_TO_BATCH,
                printRequests = listOf(buildPrintRequest(batchId = batchId))
            )
            certificateRepository.save(certificate)

            // When
            val actual = certificateRepository.findDistinctByPrintRequestStatusAndBatchId(DISPATCHED, batchId)

            // Given
            assertThat(actual).isEmpty()
        }
    }

    @Nested
    inner class FindByStatusOrderByApplicationReceivedDateTimeAsc {
        @Test
        fun `should get by status ordered by application received timestamp`() {
            // Given
            val maxUnBatchedRecordsToReturn = 10

            val status = aValidCertificateStatus()
            val certificates = (1..15).map {
                buildCertificate(
                    status = status,
                    applicationReference = "V${it.toString().padStart(9, '0')}", // V000000001 ... V0000000015
                    applicationReceivedDateTime = Instant.now().minusSeconds(it.toLong()).truncatedTo(SECONDS)
                )
            }.let {
                certificateRepository.saveAll(it)
            }

            val expected = certificates.sortedBy { it.applicationReceivedDateTime }.take(10)

            // When
            val actual = certificateRepository.findByStatusOrderByApplicationReceivedDateTimeAsc(
                status,
                Pageable.ofSize(maxUnBatchedRecordsToReturn)
            )

            // Given
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*IGNORED_FIELDS)
                .isEqualTo(expected)
        }
    }

    @Nested
    inner class FindByGssCodeInAndSourceTypeAndSourceReference {
        @Test
        fun `should find certificate given one exists for provided details`() {
            // Given
            val gssCodes = getRandomGssCodeList()
            val sourceType = aValidSourceType()
            val sourceReference = aValidSourceReference()
            var certificate = buildCertificate(
                gssCode = gssCodes[0],
                sourceType = sourceType,
                sourceReference = sourceReference
            )
            certificate = certificateRepository.save(certificate)

            // When
            val actual = certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(
                gssCodes,
                sourceType,
                sourceReference
            )

            // Then
            assertThat(actual)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*IGNORED_FIELDS)
                .isEqualTo(certificate)
        }

        @Test
        fun `should fail to find certificate given none exists for provided details`() {
            // Given
            val gssCodes = getRandomGssCodeList()
            val sourceType = aValidSourceType()
            val sourceReference = aValidSourceReference()

            // When
            val actual = certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(
                gssCodes,
                sourceType,
                sourceReference
            )

            // Then
            assertThat(actual).isNull()
        }
    }

    @Nested
    inner class GetPrintRequestStatusCount {
        @Test
        fun `should find certificates for the given range and status`() {
            // Given
            val now = Instant.now().truncatedTo(SECONDS)
            val startOfDay = now.truncatedTo(DAYS)
            val endOfDay = startOfDay.plus(1, DAYS).minusSeconds(1)
            val expected1 = buildCertificate(
                printRequests = listOf(
                    buildPrintRequest(
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
                                status = PENDING_ASSIGNMENT_TO_BATCH,
                                eventDateTime = startOfDay
                            ),
                            buildPrintRequestStatus(
                                status = Status.ASSIGNED_TO_BATCH,
                                eventDateTime = endOfDay
                            )
                        )
                    )
                )
            )

            val expected2 = buildCertificate(
                printRequests = listOf(
                    buildPrintRequest(
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
                                status = PENDING_ASSIGNMENT_TO_BATCH,
                                eventDateTime = startOfDay.minusSeconds(10)
                            )
                        )
                    ),
                    buildPrintRequest(
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
                                status = Status.ASSIGNED_TO_BATCH,
                                eventDateTime = startOfDay
                            )
                        )
                    )
                )
            )

            val other1 = buildCertificate(
                printRequests = listOf(
                    buildPrintRequest(
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
                                status = PENDING_ASSIGNMENT_TO_BATCH,
                                eventDateTime = now.plusSeconds(10)
                            )
                        )
                    )
                )
            )

            val other2 = buildCertificate(
                printRequests = listOf(
                    buildPrintRequest(
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
                                status = Status.ASSIGNED_TO_BATCH,
                                eventDateTime = startOfDay.minusSeconds(1)
                            ),
                            buildPrintRequestStatus(
                                status = Status.ASSIGNED_TO_BATCH,
                                eventDateTime = endOfDay.plusSeconds(1)
                            )
                        )
                    )
                )
            )

            certificateRepository.saveAll(listOf(other1, expected1, other2, expected2))

            // When
            val actual =
                certificateRepository.getPrintRequestStatusCount(startOfDay, endOfDay, Status.ASSIGNED_TO_BATCH)

            // Then
            assertThat(actual).isEqualTo(2)
        }
    }

    @Nested
    inner class GetCertificatesForInitialRetentionDataRemoval {
        @Test
        fun `should find certificates for removal of initial retention period data`() {
            // Given
            val expected1 = buildCertificate(
                initialRetentionRemovalDate = LocalDate.now().minusDays(1)
            )
            val expected2 = buildCertificate(
                initialRetentionRemovalDate = LocalDate.now().minusDays(1)
            )

            val other1 = buildCertificate(
                initialRetentionRemovalDate = LocalDate.now().minusDays(1),
                initialRetentionDataRemoved = true // should be excluded
            )

            val other2 = buildCertificate(
                initialRetentionRemovalDate = LocalDate.now().plusDays(1)
            )

            certificateRepository.saveAll(listOf(other1, expected1, other2, expected2))

            // When
            val actual = certificateRepository.findPendingRemovalOfInitialRetentionData(VOTER_CARD)

            // Then
            assertThat(actual).containsExactlyInAnyOrder(expected1, expected2)
        }
    }

    @Nested
    inner class GetCertificatesForFinalRetentionDataRemoval {

        @Test
        fun `should count certificates for removal of final retention period data`() {
            // Given
            val expectedCount = 2
            certificateRepository.saveAll(
                listOf(
                    buildCertificate(finalRetentionRemovalDate = LocalDate.now().minusDays(1)),
                    buildCertificate(finalRetentionRemovalDate = LocalDate.now().minusDays(1)),
                    buildCertificate(finalRetentionRemovalDate = LocalDate.now().plusDays(1)) // should not be included
                )
            )

            // When
            val count = certificateRepository.countBySourceTypeAndFinalRetentionRemovalDateBefore(VOTER_CARD)

            // Then
            assertThat(count).isEqualTo(expectedCount)
        }

        @Test
        fun `should find certificates for removal of final retention period data`() {
            // Given
            val certificate1 = buildCertificate(
                finalRetentionRemovalDate = LocalDate.now().minusDays(1)
            )
            val certificate2 = buildCertificate(
                finalRetentionRemovalDate = LocalDate.now().minusDays(1)
            )
            val certificate3 = buildCertificate(
                finalRetentionRemovalDate = LocalDate.now().plusDays(1)
            )
            certificateRepository.saveAll(listOf(certificate1, certificate2, certificate3))
            val expected1 = CertificateRemovalSummary(certificate1.id, certificate1.applicationReference) // TODO EIP1-4307 - change to photoLocationArn
            val expected2 = CertificateRemovalSummary(certificate2.id, certificate2.applicationReference) // TODO EIP1-4307 - change to photoLocationArn

            // When
            val actual = certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD, 1, 10000)

            // Then
            assertThat(actual).containsExactlyInAnyOrder(expected1, expected2)
        }
    }
}
