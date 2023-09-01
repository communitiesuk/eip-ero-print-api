package uk.gov.dluhc.printapi.database.mapper

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto
import uk.gov.dluhc.printapi.dto.PrintRequestSummaryDto
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapperImpl

@Component
class CertificateSummaryDtoMapper {

    val statusMapper: PrintRequestStatusDtoMapper = PrintRequestStatusDtoMapper()
    val deliveryAddressTypeMapper: DeliveryAddressTypeMapper = DeliveryAddressTypeMapperImpl()

    fun certificateToCertificatePrintRequestSummaryDto(certificate: Certificate): CertificateSummaryDto {
        val mostRecentPrintRequest = certificate
            .printRequests
            .sortedBy { it.requestDateTime }
            .last()
        return CertificateSummaryDto(
            vacNumber = certificate.vacNumber!!,
            applicationReference = certificate.applicationReference!!,
            sourceReference = certificate.sourceReference!!,
            firstName = mostRecentPrintRequest.firstName!!,
            middleNames = mostRecentPrintRequest.middleNames,
            surname = mostRecentPrintRequest.surname!!,
            printRequests = toPrintRequests(certificate.printRequests)
        )
    }

    private fun toPrintRequests(printRequests: MutableList<PrintRequest>): List<PrintRequestSummaryDto> =
        printRequests
            .map { printRequest -> toPrintRequestSummaryDto(printRequest) }
            .sortedByDescending { it.dateTime }

    private fun toPrintRequestSummaryDto(printRequest: PrintRequest): PrintRequestSummaryDto {
        val currentStatus = printRequest.getCurrentStatus()
        return PrintRequestSummaryDto(
            userId = printRequest.userId!!,
            status = statusMapper.toPrintRequestStatusDto(currentStatus.status!!),
            dateTime = currentStatus.eventDateTime!!,
            message = currentStatus.message,
            deliveryAddressType = printRequest.delivery?.deliveryAddressType?.let {
                deliveryAddressTypeMapper.mapEntityToDto(it)
            }
        )
    }
}
