package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker
import java.util.Date
import java.util.UUID
import kotlin.random.Random

fun buildPrintResponses(
    batchResponses: List<BatchResponse> = listOf(buildBatchResponse()),
    printResponses: List<PrintResponse> = listOf(buildPrintResponse()),
): PrintResponses =
    PrintResponses()
        .withBatchResponses(batchResponses)
        .withPrintResponses(printResponses)

private fun buildBatchResponse(
    batchId: String = Random.nextInt().toString(),
    message: String = DataFaker.faker.famousLastWords().lastWords(),
    status: BatchResponse.Status = BatchResponse.Status.SUCCESS,
    timestamp: Date = Date(),
) = BatchResponse()
    .withBatchId(batchId)
    .withMessage(message)
    .withStatus(status)
    .withTimestamp(timestamp)

private fun buildPrintResponse(
    requestId: UUID = UUID.randomUUID(),
    message: String = DataFaker.faker.famousLastWords().lastWords(),
    statusStep: PrintResponse.StatusStep = PrintResponse.StatusStep.IN_PRODUCTION,
    status: PrintResponse.Status = PrintResponse.Status.SUCCESS,
    timestamp: Date = Date(),
) = PrintResponse()
    .withMessage(message)
    .withRequestId(requestId)
    .withStatusStep(statusStep)
    .withStatus(status)
    .withTimestamp(timestamp)
