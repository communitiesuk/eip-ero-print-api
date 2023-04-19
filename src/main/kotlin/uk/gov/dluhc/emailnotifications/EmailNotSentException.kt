package uk.gov.dluhc.emailnotifications

class EmailNotSentException : RuntimeException {
    constructor(
        toRecipients: Set<String>,
        ccRecipients: Set<String>,
        subject: String,
        cause: Throwable,
    ) : super("Failed to send email to [$toRecipients], cc[$ccRecipients] with subject[$subject]", cause)

    constructor(message: String) : super(message)
}
