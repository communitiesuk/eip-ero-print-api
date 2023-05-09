package uk.gov.dluhc.emailnotifications.testsupport

import software.amazon.awssdk.services.ses.model.SendEmailResponse

fun buildSendEmailResponse(messageId: String): SendEmailResponse =
    SendEmailResponse.builder().messageId(messageId).build()
