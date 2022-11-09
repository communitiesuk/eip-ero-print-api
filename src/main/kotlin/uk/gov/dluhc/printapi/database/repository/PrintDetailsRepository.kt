package uk.gov.dluhc.printapi.database.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Expression
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import uk.gov.dluhc.printapi.config.DynamoDbConfiguration
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintDetails.Companion.REQUEST_ID_INDEX_NAME
import uk.gov.dluhc.printapi.database.entity.PrintDetails.Companion.STATUS_BATCH_ID_INDEX_NAME
import uk.gov.dluhc.printapi.database.entity.Status
import java.util.UUID

@Repository
class PrintDetailsRepository(client: DynamoDbEnhancedClient, tableConfig: DynamoDbConfiguration) {

    companion object {
        private val tableSchema = TableSchema.fromBean(PrintDetails::class.java)
    }

    private val table = client.table(tableConfig.printDetailsTableName, tableSchema)

    fun save(printDetails: PrintDetails) {
        table.putItem(printDetails)
    }

    fun get(id: UUID): PrintDetails {
        try {
            return table.getItem(key(id.toString()))
        } catch (ex: NullPointerException) {
            throw PrintDetailsNotFoundException(id)
        }
    }

    fun getByRequestId(requestId: String): PrintDetails {
        val queryConditional = QueryConditional.keyEqualTo(key(requestId))
        val index = table.index(REQUEST_ID_INDEX_NAME)
        val query = QueryEnhancedRequest.builder().queryConditional(queryConditional).build()

        try {
            return index.query(query).flatMap { it.items() }.first()
        } catch (ex: NoSuchElementException) {
            throw PrintDetailsNotFoundException(requestId)
        }
    }

    fun getAllByStatusAndBatchId(status: Status, batchId: String): List<PrintDetails> {
        val queryConditional = QueryConditional.keyEqualTo(key(status.toString(), batchId))
        val index = table.index(STATUS_BATCH_ID_INDEX_NAME)
        val query = QueryEnhancedRequest.builder().queryConditional(queryConditional).build()
        return index.query(query).flatMap { it.items() }
    }

    fun getAllByStatus(status: Status): List<PrintDetails> {
        val expression = Expression.builder()
            .expression("#print_status = :status")
            .expressionNames(mapOf(Pair("#print_status", "status")))
            .expressionValues(
                mapOf(
                    Pair(":status", AttributeValue.builder().s(Status.PENDING_ASSIGNMENT_TO_BATCH.toString()).build())
                )
            )
            .build()
        return table.scan(ScanEnhancedRequest.builder().filterExpression(expression).build()).items().toList()
    }

    fun updateItems(printList: List<PrintDetails>) {
        printList.forEach { item ->
            table.updateItem(UpdateItemEnhancedRequest.builder(PrintDetails::class.java).item(item).build())
        }
    }

    private fun key(partitionValue: String, sortValue: String): Key =
        Key.builder().partitionValue(partitionValue).sortValue(sortValue).build()

    private fun key(partitionValue: String): Key =
        Key.builder().partitionValue(partitionValue).build()
}
