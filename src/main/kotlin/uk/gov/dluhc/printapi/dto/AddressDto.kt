package uk.gov.dluhc.printapi.dto

data class AddressDto(
    var street: String,
    var postcode: String,
    var `property`: String? = null,
    var locality: String? = null,
    var town: String? = null,
    var area: String? = null,
    var uprn: String? = null,
)
