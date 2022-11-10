package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse

@Mapper(uses = [StatusStepMapper::class])
abstract class ProcessPrintResponseMessageMapper {
    abstract fun toProcessPrintResponseMessage(printResponse: PrintResponse): ProcessPrintResponseMessage
}
