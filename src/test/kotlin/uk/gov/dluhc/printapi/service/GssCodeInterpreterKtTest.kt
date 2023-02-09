package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class GssCodeInterpreterKtTest {

    companion object {
        // Example GSS Codes for different nations
        const val GSS_CODE_ENGLAND = "E09000006" // England ERO has GSS Code starting with 'E'
        const val GSS_CODE_WALES = "W06000023" // Wales ERO has GSS Code starting with 'W'
        const val GSS_CODE_SCOTLAND = "S12000011" // Scotland ERO has GSS Code starting with 'S'
        const val GSS_CODE_NORTHERN_IRELAND = "N07000001" // Northern Ireland ERO has GSS Code starting with 'N'
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "$GSS_CODE_ENGLAND, FALSE",
            "$GSS_CODE_NORTHERN_IRELAND, FALSE",
            "$GSS_CODE_SCOTLAND, FALSE",
            "$GSS_CODE_WALES, TRUE",
        ]
    )
    fun `should determine whether GSS code is for Wales ERO`(gssCode: String, expected: Boolean) {
        // Given

        // When
        val actual = isWalesCode(gssCode)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
