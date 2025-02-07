package uk.gov.dluhc.printapi.exception

import uk.gov.dluhc.printapi.database.entity.SourceType

class CertificateInitialDataRemovedException(
    eroId: String,
    sourceType: SourceType,
    sourceReference: String,
    certificateNumber: String
) : RuntimeException("Certificate for eroId = $eroId with sourceType = $sourceType and sourceReference = $sourceReference and certificateNumber = $certificateNumber has passed the initial retention period")
