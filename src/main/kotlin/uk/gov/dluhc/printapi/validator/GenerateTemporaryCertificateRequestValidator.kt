package uk.gov.dluhc.printapi.validator

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import uk.gov.dluhc.printapi.models.GenerateTemporaryCertificateRequest
import java.time.Clock
import java.time.LocalDate

@Component
class GenerateTemporaryCertificateRequestValidator(
    private val clock: Clock,
    @Value("\${api.print-api.generate-temporary-certificate.valid-on-date-maximum-advance-days}") private val maxAdvanceDays: Long
) : Validator {
    override fun supports(clazz: Class<*>): Boolean =
        GenerateTemporaryCertificateRequest::class.java == clazz

    override fun validate(target: Any, errors: Errors) {
        val generateTemporaryCertificateRequest = target as GenerateTemporaryCertificateRequest
        generateTemporaryCertificateRequest.validateValidOnDate(errors)
    }

    private fun GenerateTemporaryCertificateRequest.validateValidOnDate(errors: Errors) {
        val today = LocalDate.now(clock)
        val latestValidOnDate = today.plusDays(maxAdvanceDays)
        if (validOnDate.isBefore(today)) {
            errors.rejectValue(
                "validOnDate",
                "generateTemporaryCertificateRequest.validOnDate.minimum",
                "date cannot be in the past"
            )
        } else if (validOnDate.isAfter(latestValidOnDate)) {
            errors.rejectValue(
                "validOnDate",
                "generateTemporaryCertificateRequest.validOnDate.maximum",
                "date cannot be greater than $maxAdvanceDays in the future (cannot be after $latestValidOnDate)"
            )
        }
    }
}
