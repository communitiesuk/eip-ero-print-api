package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
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

private fun buildBatchResponse(
    batchId: String = aValidBatchId(),
    message: String = DataFaker.faker.famousLastWords().lastWords(),
    status: BatchResponse.Status = BatchResponse.Status.SUCCESS,
    timestamp: OffsetDateTime = Instant.now().atOffset(ZoneOffset.UTC),
) = BatchResponse()
    .withBatchId(batchId)
    .withMessage(message)
    .withStatus(status)
    .withTimestamp(timestamp)

private fun buildPrintResponse(
    requestId: String = aValidRequestId(),
    message: String = DataFaker.faker.famousLastWords().lastWords(),
    statusStep: PrintResponse.StatusStep = PrintResponse.StatusStep.IN_PRODUCTION,
    status: PrintResponse.Status = PrintResponse.Status.SUCCESS,
    timestamp: OffsetDateTime = Instant.now().atOffset(ZoneOffset.UTC),
) = PrintResponse()
    .withMessage(message)
    .withRequestId(requestId)
    .withStatusStep(statusStep)
    .withStatus(status)
    .withTimestamp(timestamp)
