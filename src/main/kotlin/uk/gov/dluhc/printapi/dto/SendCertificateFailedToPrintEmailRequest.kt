package uk.gov.dluhc.printapi.dto

data class SendCertificateFailedToPrintEmailRequest(
    override val sourceReference: String,
    override val applicationReference: String,
    override val localAuthorityEmailAddresses: Set<String>,
    override val requestingUserEmailAddress: String?,
) : SendEmailRequest
