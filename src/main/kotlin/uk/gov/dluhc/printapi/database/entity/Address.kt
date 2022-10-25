package uk.gov.dluhc.printapi.database.entity

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean

@DynamoDbBean
data class Address(
    var street: String? = null,
    var postcode: String? = null,
    var `property`: String? = null,
    var locality: String? = null,
    var town: String? = null,
    var area: String? = null,
    var uprn: String? = null
)
