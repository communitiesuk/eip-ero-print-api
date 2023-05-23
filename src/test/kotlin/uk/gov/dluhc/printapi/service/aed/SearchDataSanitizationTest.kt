package uk.gov.dluhc.printapi.service.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class SearchDataSanitizationTest {

    @CsvSource(
        value = [
            "doe,DOE",
            " doe ,DOE",
            "o'leary,OLEARY",
            "Llewelyn-Bowen,LLEWELYN BOWEN",
            "Harper Adams,HARPER ADAMS",
            "Harper  Adams,HARPER ADAMS",
        ]
    )
    @ParameterizedTest
    fun `should sanitize surname`(surname: String, expected: String) {
        // Given

        // When
        val actual = sanitizeSurname(surname)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @CsvSource(
        value = [
            "V123ABCXYZ,V123ABCXYZ",
            "v123abcxyz,V123ABCXYZ",
            "v  123  ab  c xy z  ,V123ABCXYZ"
        ]
    )
    @ParameterizedTest
    fun `should sanitize application reference number`(applicationReference: String, expected: String) {
        // Given

        // When
        val actual = sanitizeApplicationReference(applicationReference)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
