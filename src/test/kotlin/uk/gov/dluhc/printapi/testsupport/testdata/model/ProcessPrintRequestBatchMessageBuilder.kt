package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId

fun buildProcessPrintRequestBatchMessage(
    batchId: String = aValidBatchId(),
) = ProcessPrintRequestBatchMessage(
    batchId = batchId,
)
