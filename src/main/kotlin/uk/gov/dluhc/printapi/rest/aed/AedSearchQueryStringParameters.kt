package uk.gov.dluhc.printapi.rest.aed

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import uk.gov.dluhc.printapi.models.AedSearchBy
import java.beans.ConstructorProperties
import kotlin.reflect.KClass

@AedSearchByParametersAreValid
data class AedSearchQueryStringParameters
    @ConstructorProperties(value = ["page", "pageSize", "searchBy", "searchValue"])
    constructor(
    val page: Int = 1,
    val pageSize: Int = 100,
    val searchBy: AedSearchBy? = null,
    val searchValue: String? = null,
)

@Constraint(validatedBy = [AedSearchByParametersAreValidConstraintValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class AedSearchByParametersAreValid(
    val message: String = "searchBy and searchValue must be specified together",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class AedSearchByParametersAreValidConstraintValidator :
    ConstraintValidator<AedSearchByParametersAreValid, AedSearchQueryStringParameters> {

    override fun isValid(
        queryStringParameters: AedSearchQueryStringParameters,
        context: ConstraintValidatorContext
    ): Boolean =
        with(queryStringParameters) {
            (searchBy == null && searchValue == null) || (searchBy != null && searchValue != null)
        }
}
