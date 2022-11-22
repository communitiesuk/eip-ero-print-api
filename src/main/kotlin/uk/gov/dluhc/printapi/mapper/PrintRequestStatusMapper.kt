package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.dto.StatusDto
import uk.gov.dluhc.printapi.models.PrintRequestStatus

@Mapper
interface PrintRequestStatusMapper {
    @ValueMapping(source = "PENDING_ASSIGNMENT_TO_BATCH", target = "PRINT_MINUS_PROCESSING")
    @ValueMapping(source = "ASSIGNED_TO_BATCH", target = "PRINT_MINUS_PROCESSING")
    @ValueMapping(source = "SENT_TO_PRINT_PROVIDER", target = "PRINT_MINUS_PROCESSING")
    @ValueMapping(source = "RECEIVED_BY_PRINT_PROVIDER", target = "PRINT_MINUS_PROCESSING")
    @ValueMapping(source = "VALIDATED_BY_PRINT_PROVIDER", target = "PRINT_MINUS_PROCESSING")
    @ValueMapping(source = "IN_PRODUCTION", target = "PRINT_MINUS_PROCESSING")
    @ValueMapping(source = "DISPATCHED", target = "DISPATCHED")
    @ValueMapping(source = "NOT_DELIVERED", target = "NOT_MINUS_DELIVERED")
    @ValueMapping(source = "PRINT_PROVIDER_VALIDATION_FAILED", target = "PRINT_MINUS_FAILED")
    @ValueMapping(source = "PRINT_PROVIDER_PRODUCTION_FAILED", target = "PRINT_MINUS_PROCESSING")
    @ValueMapping(source = "PRINT_PROVIDER_DISPATCH_FAILED", target = "PRINT_MINUS_PROCESSING")
    fun toPrintRequestStatus(status: StatusDto): PrintRequestStatus
}
