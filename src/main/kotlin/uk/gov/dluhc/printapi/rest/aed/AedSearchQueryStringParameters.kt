package uk.gov.dluhc.printapi.rest.aed

import uk.gov.dluhc.printapi.models.AedSearchBy
import java.beans.ConstructorProperties

data class AedSearchQueryStringParameters @ConstructorProperties(value = ["page", "pageSize", "searchBy", "searchValue", "surname"]) constructor(
    val page: Int = 1,
    val pageSize: Int = 100,
    val searchBy: AedSearchBy? = null,
    val searchValue: String? = null,
    val surname: String? = null,
)
