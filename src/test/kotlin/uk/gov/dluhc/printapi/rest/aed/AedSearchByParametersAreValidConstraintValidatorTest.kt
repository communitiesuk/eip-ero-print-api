package uk.gov.dluhc.printapi.rest.aed

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.dluhc.printapi.models.AedSearchBy

class AedSearchByParametersAreValidConstraintValidatorTest {

    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val validator = validatorFactory.validator

    @Test
    fun `should validate with 0 violations given query string parameters without both searchBy and searchValue`() {
        // Then
        val searchCriteria = buildAedSearchQueryStringParameters(searchValue = null, searchBy = null)

        // When
        val violations: Set<ConstraintViolation<AedSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(AedSearchBy::class)
    fun `should validate with 1 violations given query string parameters have searchBy but no searchValue`(searchBy: AedSearchBy) {
        // Then
        val searchCriteria = buildAedSearchQueryStringParameters(searchValue = null, searchBy = searchBy)

        // When
        val violations: Set<ConstraintViolation<AedSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        assertThat(violations).hasSize(1)
        assertThat(violations.first().message)
            .isEqualTo("searchBy and searchValue must be specified together")
    }

    @Test
    fun `should validate with 1 violations given query string parameters have searchValue but no searchBy`() {
        // Then
        val searchCriteria = buildAedSearchQueryStringParameters(searchValue = "SMITH", searchBy = null)

        // When
        val violations: Set<ConstraintViolation<AedSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        assertThat(violations).hasSize(1)
        assertThat(violations.first().message)
            .isEqualTo("searchBy and searchValue must be specified together")
    }

    @ParameterizedTest
    @EnumSource(AedSearchBy::class)
    fun `should validate with 0 violations given query string parameters have both searchBy and searchValue`(searchBy: AedSearchBy) {
        // Then
        val searchCriteria = buildAedSearchQueryStringParameters(searchValue = "some value", searchBy = searchBy)

        // When
        val violations: Set<ConstraintViolation<AedSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        assertThat(violations).isEmpty()
    }
}

private fun buildAedSearchQueryStringParameters(
    searchBy: AedSearchBy? = null,
    searchValue: String? = null
) = AedSearchQueryStringParameters(searchBy = searchBy, searchValue = searchValue)
