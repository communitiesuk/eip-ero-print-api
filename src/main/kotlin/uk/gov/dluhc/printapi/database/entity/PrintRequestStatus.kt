package uk.gov.dluhc.printapi.database.entity

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.OffsetDateTime

@DynamoDbBean
data class PrintRequestStatus(
    var status: Status? = null,
    var dateTime: OffsetDateTime? = null,
    var message: String? = null
)
