package uk.gov.dluhc.printapi.testsupport.testdata.entity

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction

fun buildPageRequest(
    page: Int = 1,
    size: Int = 100
): PageRequest {
    val sortByIssueDateDesc = Sort.by(Direction.DESC, "issueDate")
    val sortBySurnameAsc = Sort.by(Direction.ASC, "surname")
    return PageRequest.of(page - 1, size, sortByIssueDateDesc.and(sortBySurnameAsc))
}
