package uk.gov.dluhc.printapi.exception

import uk.gov.dluhc.printapi.database.entity.SourceType

class CertificateNotFoundException(eroId: String, sourceType: SourceType, sourceReference: String) :
    RuntimeException(
        if (sourceType == SourceType.VOTER_CARD)
            "Certificate for eroId = $eroId and application id = $sourceReference not found"
        else
            "Certificate for eroId = $eroId, sourceType = $sourceType and $sourceReference not found"
    )
