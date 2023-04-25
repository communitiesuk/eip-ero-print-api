package uk.gov.dluhc.printapi.testsupport.testdata.entity

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction

fun withPageRequestAndSortOrder(
    page: Int = 0,
    size: Int = 100
): PageRequest {
    val sortByIssueDateDesc = Sort.by(Direction.DESC, "issueDate")
    val sortBySurnameAsc = Sort.by(Direction.ASC, "surname")
    return PageRequest.of(page, size, sortByIssueDateDesc.and(sortBySurnameAsc))
}
