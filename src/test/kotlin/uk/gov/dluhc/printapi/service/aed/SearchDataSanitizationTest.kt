package uk.gov.dluhc.printapi.service.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class SearchDataSanitizationTest {

    @CsvSource(
        value = [
            "doe,DOE",
            "Doe,DOE",
            "DoE,DOE",
        ]
    )
    @ParameterizedTest
    fun `should uppercase surname in sanitized form`(surname: String, expected: String) {

        val actual = sanitizeSurname(surname)

        assertThat(actual).isEqualTo(expected)
    }

    @CsvSource(
        value = [
            "doe ,DOE",
            " doe,DOE",
            " doe ,DOE",
        ]
    )
    @ParameterizedTest
    fun `should trim surname in sanitized form`(surname: String, expected: String) {

        val actual = sanitizeSurname(surname)

        assertThat(actual).isEqualTo(expected)
    }

    @CsvSource(
        value = [
            "o'leary,OLEARY",
            "de'Medici,DEMEDICI",
            "Nyong'o,NYONGO",
            "Prud'hon,PRUDHON",
        ]
    )
    @ParameterizedTest
    fun `should remove apostrophe from surname in sanitized form`(surname: String, expected: String) {

        val actual = sanitizeSurname(surname)

        assertThat(actual).isEqualTo(expected)
    }

    @CsvSource(
        value = [
            "Llewelyn-Bowen,LLEWELYN BOWEN",
            "Day-Lewis,DAY LEWIS",
            "Zeta-Jones,ZETA JONES",
            "Taylor-Joy,TAYLOR JOY",
        ]
    )
    @ParameterizedTest
    fun `should replace hyphen with space for surname in sanitized form`(surname: String, expected: String) {

        val actual = sanitizeSurname(surname)

        assertThat(actual).isEqualTo(expected)
    }

    @CsvSource(
        value = [
            "Harper Adams,HARPER ADAMS",
            "Harper  Adams,HARPER ADAMS",
            "Harper   Adams,HARPER ADAMS",
        ]
    )
    @ParameterizedTest
    fun `should reduce to single space for surname in sanitized form`(surname: String, expected: String) {

        val actual = sanitizeSurname(surname)

        assertThat(actual).isEqualTo(expected)
    }
}
