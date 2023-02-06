package uk.gov.dluhc.printapi.validator.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.exception.GenerateTemporaryCertificateValidationException
import java.time.Clock
import java.time.LocalDate

/**
 * Class to perform business rule validation against a [GenerateTemporaryCertificateDto]
 */
@Component
class GenerateTemporaryCertificateValidator(
    private val clock: Clock,
    @Value("\${api.print-api.generate-temporary-certificate.valid-on-date.max-calendar-days-in-future}") private val maxCalendarDaysInFuture: Long,
) {

    /**
     * Validates the specified GenerateTemporaryCertificateDto against business rules
     * @throws GenerateTemporaryCertificateValidationException if any business rule is breached
     */
    fun validate(generateTemporaryCertificateDto: GenerateTemporaryCertificateDto) {
        generateTemporaryCertificateDto.validateValidOnDate()
    }

    private fun GenerateTemporaryCertificateDto.validateValidOnDate() {
        val today = LocalDate.now(clock)
        val latestValidOnDate = today.plusDays(maxCalendarDaysInFuture)
        if (validOnDate.isBefore(today)) {
            throw GenerateTemporaryCertificateValidationException("Temporary Certificate validOnDate cannot be in the past")
        } else if (validOnDate.isAfter(latestValidOnDate)) {
            throw GenerateTemporaryCertificateValidationException(
                "Temporary Certificate validOnDate cannot be greater than $maxCalendarDaysInFuture in the future (cannot be after $latestValidOnDate)"
            )
        }
    }
}
