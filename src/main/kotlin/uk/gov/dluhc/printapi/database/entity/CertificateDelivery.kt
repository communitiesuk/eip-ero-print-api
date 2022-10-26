package uk.gov.dluhc.printapi.database.entity

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean

@DynamoDbBean
data class CertificateDelivery(
    var addressee: String? = null,
    var address: Address? = null,
    var deliveryClass: DeliveryClass? = null
)
