package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import java.util.UUID

internal class PrintDetailsRepositoryIntegrationTest : IntegrationTest() {

    @Nested
    inner class Save {
        @Test
        fun `should save and get print details by id`() {
            // Given
            val details = buildPrintDetails()

            // When
            printDetailsRepository.save(details)

            // Then
            val saved = printDetailsRepository.get(details.id!!)
            assertThat(saved).usingRecursiveComparison().isEqualTo(details)
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `should throw exception given non-existing print details`() {
            // Given
            val id = UUID.randomUUID()

            // When
            val ex = Assertions.catchThrowableOfType(
                { printDetailsRepository.get(id) },
                PrintDetailsNotFoundException::class.java
            )

            // Then
            assertThat(ex).isNotNull.hasMessage("Print details not found for id: $id")
        }
    }

    @Nested
    inner class GetByRequestId {
        @Test
        fun `should return print details given existing print details with requestId`() {
            // Given
            val requestId = "636b927df7a28f11448ab7d4"
            val otherRequestId = "146b927df7a28f11448ab7a9"
            val expected = buildPrintDetails(requestId = requestId)
            val other = buildPrintDetails(requestId = otherRequestId)

            printDetailsRepository.save(expected)
            printDetailsRepository.save(other)

            // When
            val actual = printDetailsRepository.getByRequestId(requestId)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        }

        @Test
        fun `should throw exception given non-existing print details for requestId`() {
            // Given
            val requestId = "636b927df7a28f11448ab7d4"

            // When
            val ex = Assertions.catchThrowableOfType(
                { printDetailsRepository.getByRequestId(requestId) },
                PrintDetailsNotFoundException::class.java
            )

            // Then
            assertThat(ex).isNotNull.hasMessage("Print details not found for requestId: $requestId")
        }
    }

    @Nested
    inner class GetAllByStatusAndBatchId {
        @Test
        fun `should get print details matching the status and batchID`() {
            // Given
            val details = buildPrintDetails()
            printDetailsRepository.save(details)

            // When
            val results = printDetailsRepository.getAllByStatusAndBatchId(details.status!!, details.batchId!!)

            // Then
            assertThat(results).containsExactly(details)
        }

        @Test
        fun `should find no matching print details given different status`() {
            // Given
            val differentStatus = Status.SENT_TO_PRINT_PROVIDER
            val details = buildPrintDetails(status = Status.PENDING_ASSIGNMENT_TO_BATCH)
            printDetailsRepository.save(details)

            // When
            val results = printDetailsRepository.getAllByStatusAndBatchId(differentStatus, details.batchId!!)

            // Then
            assertThat(results).isEmpty()
        }

        @Test
        fun `should find no matching print details given different batchId`() {
            // Given
            val differentBatchId = UUID.randomUUID().toString()
            val details = buildPrintDetails()
            printDetailsRepository.save(details)

            // When
            val results = printDetailsRepository.getAllByStatusAndBatchId(details.status!!, differentBatchId)

            // Then
            assertThat(results).isEmpty()
        }

        @Test
        fun `should get all print details by status`() {
            // Given
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = Status.PENDING_ASSIGNMENT_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = Status.PENDING_ASSIGNMENT_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = Status.PENDING_ASSIGNMENT_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = Status.ASSIGNED_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = Status.SENT_TO_PRINT_PROVIDER))

            // When
            val results = printDetailsRepository.getAllByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)

            // Then
            assertThat(results).hasSize(3)
        }
    }

    @Nested
    inner class UpdateItems {
        @Test
        fun `should update item as it exists in the repository`() {
            // Given
            val initialStatus = Status.PENDING_ASSIGNMENT_TO_BATCH
            val details = buildPrintDetails(status = initialStatus)
            printDetailsRepository.save(details)
            val updatedStatus = Status.SENT_TO_PRINT_PROVIDER
            details.addStatus(updatedStatus)

            // When
            printDetailsRepository.updateItems(listOf(details))

            // Then
            val updatedDetails = printDetailsRepository.get(details.id!!)
            assertThat(updatedDetails.status).isEqualTo(updatedStatus)
        }
    }
}
