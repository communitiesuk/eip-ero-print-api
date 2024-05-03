package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEventMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun buildPrintResponses(
    batchResponses: List<BatchResponse> = listOf(buildBatchResponse()),
    printResponses: List<PrintResponse> = listOf(buildPrintResponse()),
): PrintResponses =
    PrintResponses()
        .withBatchResponses(batchResponses)
        .withPrintResponses(printResponses)

fun buildBatchResponse(
    batchId: String = aValidBatchId(),
    status: BatchResponse.Status = BatchResponse.Status.SUCCESS,
    message: String? = if (status == BatchResponse.Status.SUCCESS) null else aValidEventMessage(),
    timestamp: OffsetDateTime = Instant.now().atOffset(ZoneOffset.UTC),
): BatchResponse = BatchResponse()
    .withBatchId(batchId)
    .withMessage(message)
    .withStatus(status)
    .withTimestamp(timestamp)

fun buildPrintResponse(
    requestId: String = aValidRequestId(),
    statusStep: PrintResponse.StatusStep = PrintResponse.StatusStep.IN_PRODUCTION,
    status: PrintResponse.Status = PrintResponse.Status.SUCCESS,
    message: String? = if (status == PrintResponse.Status.SUCCESS) null else aValidEventMessage(),
    timestamp: OffsetDateTime = Instant.now().atOffset(ZoneOffset.UTC),
): PrintResponse = PrintResponse()
    .withMessage(message)
    .withRequestId(requestId)
    .withStatusStep(statusStep)
    .withStatus(status)
    .withTimestamp(timestamp)
