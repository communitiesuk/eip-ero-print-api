package uk.gov.dluhc.printapi.rest

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import uk.gov.dluhc.printapi.models.CertificateSearchBy
import java.beans.ConstructorProperties
import kotlin.reflect.KClass

@CertificateSearchByParametersAreValid
data class CertificateSearchQueryStringParameters @ConstructorProperties(value = ["page", "pageSize", "searchBy", "searchValue"]) constructor(
    val page: Int = 1,
    val pageSize: Int = 100,
    val searchBy: CertificateSearchBy? = null,
    val searchValue: String? = null,
)

@Constraint(validatedBy = [CertificateSearchByParametersAreValidConstraintValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CertificateSearchByParametersAreValid(
    val message: String = "searchBy and searchValue must be specified together",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class CertificateSearchByParametersAreValidConstraintValidator :
    ConstraintValidator<CertificateSearchByParametersAreValid, CertificateSearchQueryStringParameters> {

    override fun isValid(
        queryStringParameters: CertificateSearchQueryStringParameters,
        context: ConstraintValidatorContext
    ): Boolean =
        with(queryStringParameters) {
            (searchBy == null && searchValue == null) || (searchBy != null && searchValue != null)
        }
}
