package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.models.CertificateStatisticsStatus

@Mapper
interface CertificateStatisticsStatusMapper {
    fun fromEntityPrintRequestStatus(status: PrintRequestStatus.Status): CertificateStatisticsStatus
}
