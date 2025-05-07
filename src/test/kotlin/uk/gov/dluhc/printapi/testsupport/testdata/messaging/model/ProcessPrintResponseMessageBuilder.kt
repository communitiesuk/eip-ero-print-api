package uk.gov.dluhc.printapi.testsupport.testdata.messaging.model

import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEventMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC

fun buildProcessPrintResponseMessage(
    requestId: String = aValidRequestId(),
    timestamp: OffsetDateTime = Instant.now().atOffset(UTC),
    statusStep: ProcessPrintResponseMessage.StatusStep = ProcessPrintResponseMessage.StatusStep.PROCESSED,
    status: ProcessPrintResponseMessage.Status = ProcessPrintResponseMessage.Status.SUCCESS,
    message: String? = if (status == ProcessPrintResponseMessage.Status.SUCCESS) null else aValidEventMessage(),
) = ProcessPrintResponseMessage(
    requestId = requestId,
    timestamp = timestamp,
    statusStep = statusStep,
    status = status,
    message = message
)
