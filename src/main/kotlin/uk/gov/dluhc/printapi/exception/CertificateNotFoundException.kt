package uk.gov.dluhc.printapi.exception

import uk.gov.dluhc.printapi.database.entity.SourceType

class CertificateNotFoundException : RuntimeException {
    constructor(eroId: String, sourceType: SourceType, sourceReference: String) :
        super("Certificate for eroId = $eroId with sourceType = $sourceType and sourceReference = $sourceReference not found")

    constructor(sourceType: SourceType, sourceReference: String) :
        super("Certificate with sourceType = $sourceType and sourceReference = $sourceReference not found")
}
