package uk.gov.dluhc.printapi.database.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest
import uk.gov.dluhc.printapi.config.DynamoDbConfiguration
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintDetails.Companion.STATUS_BATCH_ID_INDEX_NAME
import uk.gov.dluhc.printapi.database.entity.PrintDetails.Companion.STATUS_INDEX_NAME
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

    fun getAllByStatusAndBatchId(status: Status, batchId: String): List<PrintDetails> {
        val queryConditional = QueryConditional.keyEqualTo(key(status.toString(), batchId))
        val index = table.index(STATUS_BATCH_ID_INDEX_NAME)
        val query = QueryEnhancedRequest.builder().queryConditional(queryConditional).build()
        return index.query(query).flatMap { it.items() }
    }

    fun filterItemsBy(status: Status): List<PrintDetails> {
//        return table.scan().flatMap { it.items() }.filter { it.status == Status.PENDING_ASSIGNMENT_TO_BATCH }
        val queryConditional = QueryConditional.keyEqualTo(key(status.toString()))
        val index = table.index(STATUS_INDEX_NAME)
        val query = QueryEnhancedRequest.builder().queryConditional(queryConditional).build()
        return index.query(query).flatMap { it.items() }
    }

    private fun key(partitionValue: String, sortValue: String): Key =
        Key.builder().partitionValue(partitionValue).sortValue(sortValue).build()
    private fun key(partitionValue: String): Key =
        Key.builder().partitionValue(partitionValue).build()
}
