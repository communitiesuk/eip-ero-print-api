package uk.gov.dluhc.printapi.messaging.service

import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage

abstract class AbstractEmailSender(
    private val electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient,
) {
    /**
     * @param printResponse the printResponse that could trigger an email being sent
     * @param certificate the certificate associated to the printResponse
     * @return true if an email was sent otherwise false
     */
    abstract fun testAndSend(
        printResponse: ProcessPrintResponseMessage,
        certificate: Certificate
    ): Boolean

    protected fun getLocalAuthorityEmailAddress(gssCode: String): String {
        val ero = electoralRegistrationOfficeManagementApiClient.getEro(gssCode)
        return ero.englishContactDetails.emailAddress
    }

    protected fun getRequestingUserEmailAddress(
        printRequests: MutableList<PrintRequest>,
        printResponseRequestId: String
    ) = printRequests.find { it.requestId == printResponseRequestId }?.userId
}
