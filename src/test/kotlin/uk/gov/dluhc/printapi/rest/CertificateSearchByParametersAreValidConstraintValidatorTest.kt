package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.dluhc.printapi.models.CertificateSearchBy
import javax.validation.ConstraintViolation
import javax.validation.Validation

class CertificateSearchByParametersAreValidConstraintValidatorTest {

    private val validatorFactory = Validation.buildDefaultValidatorFactory()
    private val validator = validatorFactory.validator

    @Test
    fun `should validate with 0 violations given query string parameters without both searchBy and searchValue`() {
        // Then
        val searchCriteria = buildCertificateSearchQueryStringParameters(searchValue = null, searchBy = null)

        // When
        val violations: Set<ConstraintViolation<CertificateSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        Assertions.assertThat(violations).isEmpty()
    }

    @ParameterizedTest
    @EnumSource(CertificateSearchBy::class)
    fun `should validate with 1 violations given query string parameters have searchBy but no searchValue`(searchBy: CertificateSearchBy) {
        // Then
        val searchCriteria = buildCertificateSearchQueryStringParameters(searchValue = null, searchBy = searchBy)

        // When
        val violations: Set<ConstraintViolation<CertificateSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        Assertions.assertThat(violations).hasSize(1)
        Assertions.assertThat(violations.first().message)
            .isEqualTo("searchBy and searchValue must be specified together")
    }

    @Test
    fun `should validate with 1 violations given query string parameters have searchValue but no searchBy`() {
        // Then
        val searchCriteria = buildCertificateSearchQueryStringParameters(searchValue = "some value", searchBy = null)

        // When
        val violations: Set<ConstraintViolation<CertificateSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        Assertions.assertThat(violations).hasSize(1)
        Assertions.assertThat(violations.first().message)
            .isEqualTo("searchBy and searchValue must be specified together")
    }

    @ParameterizedTest
    @EnumSource(CertificateSearchBy::class)
    fun `should validate with 0 violations given query string parameters have both searchBy and searchValue`(searchBy: CertificateSearchBy) {
        // Then
        val searchCriteria = buildCertificateSearchQueryStringParameters(searchValue = "some value", searchBy = searchBy)

        // When
        val violations: Set<ConstraintViolation<CertificateSearchQueryStringParameters>> =
            validator.validate(searchCriteria)

        // Then
        Assertions.assertThat(violations).isEmpty()
    }
}

private fun buildCertificateSearchQueryStringParameters(
    searchBy: CertificateSearchBy? = null,
    searchValue: String? = null
) = CertificateSearchQueryStringParameters(searchBy = searchBy, searchValue = searchValue)
