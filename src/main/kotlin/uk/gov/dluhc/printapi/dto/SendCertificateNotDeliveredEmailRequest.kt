package uk.gov.dluhc.printapi.dto

data class SendCertificateNotDeliveredEmailRequest(
    val sourceReference: String,
    val applicationReference: String,
    val localAuthorityEmailAddresses: Set<String>,
    val requestingUserEmailAddress: String?,
)
