package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AedMappingHelperTest {

    companion object {
        private const val FIXED_DATE_STRING = "2022-10-18"
        private val FIXED_DATE = LocalDate.parse(FIXED_DATE_STRING)
        private val FIXED_TIME = Instant.parse("${FIXED_DATE_STRING}T11:22:32.123Z")
        private val FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        private val ID_FIELDS_REGEX = ".*id"
    }

    private val aedMappingHelper = AedMappingHelper(FIXED_CLOCK)

    @Test
    fun `should generate request date time`() {
        // Given

        // When
        val actual = aedMappingHelper.requestDateTime()

        // Then
        assertThat(actual).isEqualTo(FIXED_TIME)
    }

    @Test
    fun `should generate issue date`() {
        // Given

        // When
        val actual = aedMappingHelper.issueDate()

        // Then
        assertThat(actual).isEqualTo(FIXED_DATE)
    }

    @Test
    fun `should generate status history`() {
        // Given
        val expected = listOf(
            AnonymousElectorDocumentStatus(
                status = AnonymousElectorDocumentStatus.Status.PRINTED,
                eventDateTime = FIXED_TIME
            )
        )

        // When
        val actual = aedMappingHelper.statusHistory(AnonymousElectorDocumentStatus.Status.PRINTED)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFieldsMatchingRegexes(ID_FIELDS_REGEX).isEqualTo(expected)
    }
}
