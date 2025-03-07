package uk.gov.dluhc.printapi.exception

class UpdateAnonymousElectorDocumentAllInitialDataRemovedException(
    eroId: String,
    sourceReference: String,
) : RuntimeException("All certificate for eroId = $eroId with sourceReference = $sourceReference have pass the initial retention period and cannot be updated.")
