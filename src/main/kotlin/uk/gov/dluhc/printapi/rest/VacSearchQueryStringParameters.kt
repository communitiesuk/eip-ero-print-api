package uk.gov.dluhc.printapi.rest

import uk.gov.dluhc.printapi.models.VacSearchBy
import java.beans.ConstructorProperties
import javax.validation.Constraint
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext
import javax.validation.Payload
import kotlin.reflect.KClass

@VacSearchByParametersAreValid
data class VacSearchQueryStringParameters @ConstructorProperties(value = ["page", "pageSize", "searchBy", "searchValue"]) constructor(
    val page: Int = 1,
    val pageSize: Int = 100,
    val searchBy: VacSearchBy? = null,
    val searchValue: String? = null,
)

@Constraint(validatedBy = [VacSearchByParametersAreValidConstraintValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class VacSearchByParametersAreValid(
    val message: String = "searchBy and searchValue must be specified together",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

class VacSearchByParametersAreValidConstraintValidator :
    ConstraintValidator<VacSearchByParametersAreValid, VacSearchQueryStringParameters> {

    override fun isValid(
        queryStringParameters: VacSearchQueryStringParameters,
        context: ConstraintValidatorContext
    ): Boolean =
        with(queryStringParameters) {
            (searchBy == null && searchValue == null) || (searchBy != null && searchValue != null)
        }
}
