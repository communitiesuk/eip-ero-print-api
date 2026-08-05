package uk.gov.dluhc.printapi.messaging.stubs

import mu.KotlinLogging
import uk.gov.dluhc.messagingsupport.MessageListener

private val logger = KotlinLogging.logger {}

abstract class MessageListenerStub<T> : MessageListener<T> {

    private val messagesReceived = mutableListOf<T>()

    override fun handleMessage(payload: T) {
        logger.info { "received message: $payload" }
        messagesReceived += payload
    }

    fun getMessages() = messagesReceived.toList()

    fun clear() {
        messagesReceived.clear()
    }
}
