package uk.gov.dluhc.printapi.mapper

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.NOT_DELIVERED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PRINTED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PRINT_PROVIDER_VALIDATION_FAILED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.VALIDATED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status as StatusEntityEnum
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.Status as StatusModelEnum

@Component
class StatusMapper {

    fun toStatusEntityEnum(statusStep: StatusStep, statusModelEnum: StatusModelEnum): StatusEntityEnum =
        when (statusModelEnum to statusStep) {
            StatusModelEnum.SUCCESS to StatusStep.PROCESSED -> VALIDATED_BY_PRINT_PROVIDER
            StatusModelEnum.SUCCESS to StatusStep.IN_MINUS_PRODUCTION -> IN_PRODUCTION
            StatusModelEnum.SUCCESS to StatusStep.PRINTED -> PRINTED
            StatusModelEnum.SUCCESS to StatusStep.DISPATCHED -> DISPATCHED
            StatusModelEnum.FAILED to StatusStep.PROCESSED -> PRINT_PROVIDER_VALIDATION_FAILED
            StatusModelEnum.FAILED to StatusStep.NOT_MINUS_DELIVERED -> NOT_DELIVERED
            else -> throw IllegalArgumentException(
                "Print status cannot be in statusStep [$statusStep] when the status is [$statusModelEnum]"
            )
        }
}
