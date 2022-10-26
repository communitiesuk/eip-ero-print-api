package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
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
}
