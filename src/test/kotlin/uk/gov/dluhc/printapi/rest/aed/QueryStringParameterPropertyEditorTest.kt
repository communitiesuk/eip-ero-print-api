package uk.gov.dluhc.printapi.rest.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class QueryStringParameterPropertyEditorTest {

    private val propertyEditor = QueryStringParameterPropertyEditor()

    @ParameterizedTest
    @MethodSource("valuesAndExpectedValues")
    fun `should set as text`(originalValue: String?, expectedValue: String?) {
        // Given

        // When
        propertyEditor.asText = originalValue

        // Then
        assertThat(propertyEditor.value).isEqualTo(expectedValue)
    }

    companion object {
        @JvmStatic
        fun valuesAndExpectedValues(): List<Arguments> {
            return listOf(
                Arguments.of(null, null),
                Arguments.of("", null),
                Arguments.of("  ", null),
                Arguments.of("%20", null),
                Arguments.of("singleWord", "singleWord"),
                Arguments.of("hyphenated-word", "hyphenated-word"),
                Arguments.of("  words with spaces   ", "words with spaces"),
                Arguments.of("URL%20Encoded%20String", "URL Encoded String"),
                Arguments.of("%20%20URL%20Encoded%20String%20%20%20", "URL Encoded String"),
            )
        }
    }
}
