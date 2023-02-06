package uk.gov.dluhc.printapi.exception

class TemporaryCertificateExplainerDocumentNotFoundException(gssCode: String) :
    RuntimeException("Temporary certificate explainer document not found for gssCode $gssCode")
