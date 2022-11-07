package uk.gov.dluhc.printapi.database.entity

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
import java.time.OffsetDateTime

@DynamoDbBean
data class PrintRequestStatus(
    var status: Status? = null,
    var dateCreated: OffsetDateTime? = null,
    var eventDateTime: OffsetDateTime? = null, // either the "timestamp" from the print provider, or the current time
    var message: String? = null
)
