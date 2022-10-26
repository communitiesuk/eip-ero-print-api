package uk.gov.dluhc.printapi.database.repository

import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.enhanced.dynamodb.Key
import software.amazon.awssdk.enhanced.dynamodb.TableSchema
import uk.gov.dluhc.printapi.config.DynamoDbConfiguration
import uk.gov.dluhc.printapi.database.entity.PrintDetails
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

    private fun key(partitionValue: String): Key =
        Key.builder().partitionValue(partitionValue).build()
}
