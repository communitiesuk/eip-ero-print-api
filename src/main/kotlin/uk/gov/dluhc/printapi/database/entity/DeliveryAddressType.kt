package uk.gov.dluhc.printapi.database.entity

enum class DeliveryAddressType {
    REGISTERED, // The applicant's registered address
    ERO_COLLECTION, // The ERO's address; for collection
    ALTERNATIVE, // An alternative address, such as overseas or BFPO
}
