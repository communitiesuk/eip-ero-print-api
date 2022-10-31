package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class IdFactoryTest {

    private val idFactory = IdFactory()

    @Nested
    inner class RequestId {
        @Test
        fun `should generate requestId`() {
            // Given

            // When
            val requestId = idFactory.requestId()

            // Then
            assertThat(requestId).hasSize(24)
        }

        @Test
        fun `should generate unique requestId with each call`() {
            // Given
            val requestIds = mutableSetOf<String>() // store request IDs in a set to ensure unique entries

            // When
            repeat(10) { requestIds.add(idFactory.requestId()) }

            // Then
            assertThat(requestIds).hasSize(10) // 10 elements in the set mean that there were no duplicates
        }
    }

    @Nested
    inner class VacNumber {
        @Test
        fun `should generate unique vacNumber`() {
            // Given
            val vacNumbers = mutableListOf<String>()

            // When
            repeat(100) { vacNumbers.add(idFactory.vacNumber()) }

            // Then
            assertThat(vacNumbers).doesNotHaveDuplicates()
                .allSatisfy { assertThat(it).containsPattern(Regex("^[0-9AC-HJ-NP-RT-Z]{20}$").pattern) }
        }
    }
}
