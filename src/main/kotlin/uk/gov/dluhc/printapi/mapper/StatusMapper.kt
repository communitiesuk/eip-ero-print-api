package uk.gov.dluhc.printapi.mapper

import uk.gov.dluhc.printapi.database.entity.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.Status.NOT_DELIVERED
import uk.gov.dluhc.printapi.database.entity.Status.PRINT_PROVIDER_DISPATCH_FAILED
import uk.gov.dluhc.printapi.database.entity.Status.PRINT_PROVIDER_PRODUCTION_FAILED
import uk.gov.dluhc.printapi.database.entity.Status.PRINT_PROVIDER_VALIDATION_FAILED
import uk.gov.dluhc.printapi.database.entity.Status.VALIDATED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep
import uk.gov.dluhc.printapi.database.entity.Status as StatusEntityEnum
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.Status as StatusModelEnum

class StatusMapper {

    fun toStatusEntityEnum(statusStep: StatusStep, statusModelEnum: StatusModelEnum): StatusEntityEnum {
        if (statusModelEnum == StatusModelEnum.SUCCESS) {
            return when (statusStep) {
                StatusStep.PROCESSED -> VALIDATED_BY_PRINT_PROVIDER
                StatusStep.IN_MINUS_PRODUCTION -> IN_PRODUCTION
                StatusStep.DISPATCHED -> DISPATCHED
                else -> throw IllegalArgumentException("Undefined statusStep [$statusStep] and status [SUCCESS] combination")
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
