package uk.gov.dluhc.printapi.database.mapper

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.dto.VacPrintRequestSummaryDto
import uk.gov.dluhc.printapi.dto.VacSummaryDto

@Component
class VacSummaryDtoMapper {

    val statusMapper: PrintRequestStatusDtoMapper = PrintRequestStatusDtoMapper()

    fun certificateToVacSummaryDto(certificate: Certificate): VacSummaryDto {
        val mostRecentPrintRequest = certificate
            .printRequests
            .sortedBy { it.dateCreated }
            .last()
        return VacSummaryDto(
            vacNumber = certificate.vacNumber!!,
            applicationReference = certificate.applicationReference!!,
            sourceReference = certificate.sourceReference!!,
            printRequests = toPrintRequests(certificate.printRequests),
            firstName = mostRecentPrintRequest.firstName!!,
            middleNames = mostRecentPrintRequest.middleNames,
            surname = mostRecentPrintRequest.surname!!,
        )
    }

    private fun toPrintRequests(printRequests: MutableList<PrintRequest>): List<VacPrintRequestSummaryDto> =
        printRequests
            .map { printRequest -> toPrintRequestSummaryDto(printRequest) }
            .sortedByDescending { it.dateTime }

    private fun toPrintRequestSummaryDto(printRequest: PrintRequest): VacPrintRequestSummaryDto {
        val currentStatus = printRequest.getCurrentStatus()
        return VacPrintRequestSummaryDto(
            userId = printRequest.userId!!,
            status = statusMapper.toPrintRequestStatusDto(currentStatus.status!!),
            dateTime = printRequest.requestDateTime!!,
        )
    }
}
