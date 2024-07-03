package uk.gov.dluhc.printapi.exception

class ResponseFileTooLargeException : RuntimeException {
    constructor(eroId: String, documentType: String, sourceReference: String) :
        super("Response file for eroId = $eroId and sourceReference = $sourceReference too large to generate $documentType")
}
