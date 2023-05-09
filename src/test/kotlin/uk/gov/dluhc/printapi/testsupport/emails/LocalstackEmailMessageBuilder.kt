package uk.gov.dluhc.printapi.testsupport.emails

import java.time.LocalDateTime

fun buildLocalstackEmailMessage(
    emailSender: String,
    toAddresses: Set<String>,
    subject: String,
    htmlBody: String,
    timestamp: LocalDateTime,
): LocalstackEmailMessage =
    LocalstackEmailMessage(
        source = emailSender,
        destination = LocalstackEmailDestination(toAddresses = toAddresses),
        subject = subject,
        body = LocalstackEmailBody(htmlPart = htmlBody),
        timestamp = timestamp
    )
