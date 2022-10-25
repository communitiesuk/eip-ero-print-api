package uk.gov.dluhc.printapi.database.entity

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean

@DynamoDbBean
data class ElectoralRegistrationOffice(
    var name: String? = null,
    var phoneNumber: String? = null,
    var emailAddress: String? = null,
    var website: String? = null,
    var address: Address? = null
)
