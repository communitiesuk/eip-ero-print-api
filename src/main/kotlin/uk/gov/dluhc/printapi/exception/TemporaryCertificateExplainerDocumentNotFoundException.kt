package uk.gov.dluhc.printapi.exception

class TemporaryCertificateExplainerDocumentNotFoundException(eroId: String, gssCode: String) :
    RuntimeException("Temporary certificate explainer document not found for eroId $eroId and gssCode $gssCode")
