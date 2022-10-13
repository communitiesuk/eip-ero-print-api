package uk.gov.dluhc.printapi.messaging

interface MessageListener<PAYLOAD> {
    fun handleMessage(payload: PAYLOAD)
}
