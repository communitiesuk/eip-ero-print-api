package uk.gov.dluhc.printapi.dto

enum class PrintRequestStatusDto {
    PENDING_ASSIGNMENT_TO_BATCH,
    ASSIGNED_TO_BATCH,
    SENT_TO_PRINT_PROVIDER,
    RECEIVED_BY_PRINT_PROVIDER,
    VALIDATED_BY_PRINT_PROVIDER,
    IN_PRODUCTION,
    DISPATCHED,
    NOT_DELIVERED,
    PRINT_PROVIDER_VALIDATION_FAILED,
    PRINT_PROVIDER_PRODUCTION_FAILED,
    PRINT_PROVIDER_DISPATCH_FAILED,
}
