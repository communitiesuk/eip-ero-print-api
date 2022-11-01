package uk.gov.dluhc.printapi.messaging.models

data class ProcessPrintBatchStatusUpdateMessage(
    private val fileDirectory: String,
    private val fileName: String
)
