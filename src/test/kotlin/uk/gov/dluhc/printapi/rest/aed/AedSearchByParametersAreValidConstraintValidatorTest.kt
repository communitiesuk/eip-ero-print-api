package uk.gov.dluhc.printapi.rest.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.models.AedSearchBy
import uk.gov.dluhc.printapi.models.AedSearchBy.SURNAME
import javax.validation.ConstraintViolation
import javax.validation.Validation

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

    @Test
    fun `should validate with 1 violations given query string parameters have searchBy but no searchValue`() {
        // Then
        val searchCriteria = buildAedSearchQueryStringParameters(searchValue = null, searchBy = SURNAME)

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

    @Test
    fun `should validate with 0 violations given query string parameters have both searchBy and searchValue`() {
        // Then
        val searchCriteria = buildAedSearchQueryStringParameters(searchValue = "SMITH", searchBy = SURNAME)

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
