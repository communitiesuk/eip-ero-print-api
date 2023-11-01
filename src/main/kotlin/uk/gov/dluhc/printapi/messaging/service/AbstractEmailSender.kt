package uk.gov.dluhc.printapi.messaging.service

import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage

abstract class AbstractEmailSender(
    private val electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient,
) {
    abstract fun send(
        printResponse: ProcessPrintResponseMessage,
        certificate: Certificate
    )

    protected fun getLocalAuthorityEmailAddress(gssCode: String): String {
        val ero = electoralRegistrationOfficeManagementApiClient.getEro(gssCode)
        return ero.englishContactDetails.emailAddressVac ?: ero.englishContactDetails.emailAddress
    }

    protected fun getRequestingUserEmailAddress(
        printRequests: MutableList<PrintRequest>,
        printResponseRequestId: String
    ) = printRequests.find { it.requestId == printResponseRequestId }?.userId
}
