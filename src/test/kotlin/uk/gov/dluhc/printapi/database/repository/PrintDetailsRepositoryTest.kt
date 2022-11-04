package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

internal class PrintDetailsRepositoryTest : IntegrationTest() {

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
            val differentStatus = SENT_TO_PRINT_PROVIDER
            val details = buildPrintDetails(
                printRequestStatuses = mutableListOf(
                    PrintRequestStatus(PENDING_ASSIGNMENT_TO_BATCH, OffsetDateTime.now(ZoneOffset.UTC))
                )
            )
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
        fun `should find eligible print requests`() {
            // Given
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = PENDING_ASSIGNMENT_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = PENDING_ASSIGNMENT_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = PENDING_ASSIGNMENT_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = ASSIGNED_TO_BATCH))
            printDetailsRepository.save(buildPrintDetails(batchId = null, status = SENT_TO_PRINT_PROVIDER))

            // When
            val results = printDetailsRepository.getAllByStatus(PENDING_ASSIGNMENT_TO_BATCH)

            // Then
            assertThat(results).hasSize(3)
        }
    }

    @Test
    fun `should update item as it exists in the repository`() {
        // Given
        val initialStatus = PENDING_ASSIGNMENT_TO_BATCH
        val details = buildPrintDetails(status = initialStatus)
        printDetailsRepository.save(details)
        val updatedStatus = SENT_TO_PRINT_PROVIDER
        details.addStatus(updatedStatus)

        // When
        printDetailsRepository.updateItems(listOf(details))

        // Then
        val updatedDetails = printDetailsRepository.get(details.id!!)
        assertThat(updatedDetails.status).isEqualTo(updatedStatus)
    }
}
