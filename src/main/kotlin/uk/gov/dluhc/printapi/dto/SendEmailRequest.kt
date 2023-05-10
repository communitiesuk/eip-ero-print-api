package uk.gov.dluhc.printapi.dto

interface SendEmailRequest {
    val sourceReference: String
    val applicationReference: String
    val localAuthorityEmailAddresses: Set<String>
    val requestingUserEmailAddress: String?
}
