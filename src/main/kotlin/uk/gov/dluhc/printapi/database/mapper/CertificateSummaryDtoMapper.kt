package uk.gov.dluhc.printapi.database.mapper

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto
import uk.gov.dluhc.printapi.dto.PrintRequestSummaryDto

@Component
class CertificateSummaryDtoMapper {

    val statusMapper: PrintRequestStatusDtoMapper = PrintRequestStatusDtoMapper()

    fun certificateToCertificatePrintRequestSummaryDto(certificate: Certificate): CertificateSummaryDto {
        return CertificateSummaryDto(
            vacNumber = certificate.vacNumber!!,
            printRequests = toPrintRequests(certificate.printRequests)
        )
    }

    private fun toPrintRequests(printRequests: MutableList<PrintRequest>): List<PrintRequestSummaryDto> =
        printRequests.map { printRequest -> toPrintRequestSummaryDto(printRequest) }

    private fun toPrintRequestSummaryDto(printRequest: PrintRequest): PrintRequestSummaryDto {
        val currentStatus = printRequest.getCurrentStatus()
        return PrintRequestSummaryDto(
            userId = printRequest.userId!!,
            status = statusMapper.toPrintRequestStatusDto(currentStatus.status!!),
            dateTime = currentStatus.eventDateTime!!,
            message = currentStatus.message
        )
    }
}
