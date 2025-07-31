package uk.gov.dluhc.printapi.mapper

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.NOT_DELIVERED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PRINT_PROVIDER_DISPATCH_FAILED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PRINT_PROVIDER_PRODUCTION_FAILED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PRINT_PROVIDER_VALIDATION_FAILED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.VALIDATED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status as StatusEntityEnum
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.Status as StatusModelEnum

@Component
class StatusMapper {

    fun toStatusEntityEnum(statusStep: StatusStep, statusModelEnum: StatusModelEnum): StatusEntityEnum {
        if (statusModelEnum == StatusModelEnum.SUCCESS) {
            return when (statusStep) {
                StatusStep.PROCESSED -> VALIDATED_BY_PRINT_PROVIDER
                StatusStep.IN_MINUS_PRODUCTION -> IN_PRODUCTION
                StatusStep.DISPATCHED -> DISPATCHED
                else -> throw IllegalArgumentException("Print status cannot be in statusStep [NOT_MINUS_DELIVERED] when the status is [SUCCESS]")
            }
        }
        return when (statusStep) {
            StatusStep.NOT_MINUS_DELIVERED -> NOT_DELIVERED
            StatusStep.PROCESSED -> PRINT_PROVIDER_VALIDATION_FAILED
            StatusStep.IN_MINUS_PRODUCTION -> PRINT_PROVIDER_PRODUCTION_FAILED
            StatusStep.DISPATCHED -> PRINT_PROVIDER_DISPATCH_FAILED
        }
    }
}
