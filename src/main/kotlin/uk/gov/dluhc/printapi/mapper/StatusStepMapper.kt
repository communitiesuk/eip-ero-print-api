package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep as StatusStepMessageEnum
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse.StatusStep as StatusStepModelEnum

@Mapper
interface StatusStepMapper {
    @ValueMapping(source = "IN_PRODUCTION", target = "IN_MINUS_PRODUCTION")
    @ValueMapping(source = "NOT_DELIVERED", target = "NOT_MINUS_DELIVERED")
    fun toStatusStepMessageEnum(statusStep: StatusStepModelEnum): StatusStepMessageEnum
}
