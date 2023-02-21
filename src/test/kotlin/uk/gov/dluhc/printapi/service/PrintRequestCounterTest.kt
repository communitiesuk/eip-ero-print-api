package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.time.Instant
import java.util.UUID

class PrintRequestCounterTest {

    @Nested
    inner class CountPrintRequestsAssignedToBatch {
        @Test
        fun `should determine count given no print requests found for provided batchId`() {
            // Given
            val batchId = "4143d442a2424740afa3ce5eae630aaf"
            val otherBatchId = "bdbf4054cc904c4abf61cd6bb9171b55"
            val certificates = listOf(
                buildCertificate(
                    printRequests = listOf(
                        buildPrintRequest(
                            batchId = otherBatchId,
                            printRequestStatuses = listOf(buildPrintRequestStatus(status = ASSIGNED_TO_BATCH))
                        )
                    )
                )
            )
            val expectedPrintRequestCount = 0

            // When
            val actual = countPrintRequestsAssignedToBatch(certificates, batchId)

            // Then
            assertThat(actual).isEqualTo(expectedPrintRequestCount)
        }

        @Test
        fun `should determine count given no print requests found with current status of ASSIGNED_TO_BATCH`() {
            // Given
            val batchId = "4143d442a2424740afa3ce5eae630aaf"
            val latestEventTimestamp = Instant.now().minusSeconds(10)
            val certificates = listOf(
                buildCertificate(
                    printRequests = listOf(
                        buildPrintRequest(
                            batchId = batchId,
                            printRequestStatuses = listOf(
                                buildPrintRequestStatus(
                                    status = ASSIGNED_TO_BATCH,
                                    eventDateTime = latestEventTimestamp.minusSeconds(30)
                                ),
                                buildPrintRequestStatus(
                                    status = SENT_TO_PRINT_PROVIDER, eventDateTime = latestEventTimestamp
                                )
                            )
                        )
                    )
                ),
            )
            val expectedPrintRequestCount = 0

            // When
            val actual = countPrintRequestsAssignedToBatch(certificates, batchId)

            // Then
            assertThat(actual).isEqualTo(expectedPrintRequestCount)
        }

        @Test
        fun `should determine count given same certificate included in list`() {
            // Given
            val batchId = "4143d442a2424740afa3ce5eae630aaf"
            val otherBatchId = "bdbf4054cc904c4abf61cd6bb9171b55"
            // certificate with one print request for current batch and one historic
            val certificate = buildCertificate(
                    printRequests = listOf(
                        buildPrintRequest(
                            batchId = otherBatchId,
                            printRequestStatuses = listOf(
                                buildPrintRequestStatus(
                                    status = ASSIGNED_TO_BATCH,
                                    eventDateTime = Instant.now().minusSeconds(30)
                                ),
                                buildPrintRequestStatus(
                                    status = SENT_TO_PRINT_PROVIDER,
                                    eventDateTime = Instant.now().minusSeconds(10)
                                ),
                            )
                        ).also {
                            it.id = UUID.randomUUID()
                        },
                        buildPrintRequest(
                            batchId = batchId,
                            printRequestStatuses = listOf(
                                buildPrintRequestStatus(status = ASSIGNED_TO_BATCH),
                            )
                        ).also {
                            it.id = UUID.randomUUID()
                        },
                    )
                )
            val certificates = listOf(certificate, certificate, certificate, certificate, certificate)
            val expectedPrintRequestCount = 1

            // When
            val actual = countPrintRequestsAssignedToBatch(certificates, batchId)

            // Then
            assertThat(actual).isEqualTo(expectedPrintRequestCount)
        }

        @Test
        fun `should determine count given several certificates with differing print requests`() {
            // Given
            val batchId = "4143d442a2424740afa3ce5eae630aaf"
            val otherBatchId = "bdbf4054cc904c4abf61cd6bb9171b55"
            val certificates = listOf(
                // cert with one print request for current batch and one historic
                buildCertificate(
                    printRequests = listOf(
                        buildPrintRequest(
                            batchId = otherBatchId,
                            printRequestStatuses = listOf(
                                buildPrintRequestStatus(
                                    status = ASSIGNED_TO_BATCH,
                                    eventDateTime = Instant.now().minusSeconds(30)
                                ),
                                buildPrintRequestStatus(
                                    status = SENT_TO_PRINT_PROVIDER,
                                    eventDateTime = Instant.now().minusSeconds(10)
                                ),
                            )
                        ),
                        buildPrintRequest(
                            batchId = batchId,
                            printRequestStatuses = listOf(
                                buildPrintRequestStatus(status = ASSIGNED_TO_BATCH),
                            )
                        )
                    )
                ),
                // cert with two print requests for current batch
                buildCertificate(
                    printRequests = listOf(
                        buildPrintRequest(
                            batchId = batchId,
                            printRequestStatuses = listOf(buildPrintRequestStatus(status = ASSIGNED_TO_BATCH))
                        ),
                        buildPrintRequest(
                            batchId = batchId,
                            printRequestStatuses = listOf(buildPrintRequestStatus(status = ASSIGNED_TO_BATCH))
                        )
                    )
                ),
                // cert with one print request for current batch
                buildCertificate(
                    printRequests = listOf(
                        buildPrintRequest(
                            batchId = batchId,
                            printRequestStatuses = listOf(buildPrintRequestStatus(status = ASSIGNED_TO_BATCH))
                        )
                    )
                ),
            )
            val expectedPrintRequestCount = 4

            // When
            val actual = countPrintRequestsAssignedToBatch(certificates, batchId)

            // Then
            assertThat(actual).isEqualTo(expectedPrintRequestCount)
        }
    }
}
