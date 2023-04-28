package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.database.entity.SourceType
import java.util.UUID

@Repository
interface AnonymousElectorDocumentSummaryRepository :
    PagingAndSortingRepository<AnonymousElectorDocumentSummary, UUID> {

    fun findAllByGssCodeInAndSourceType(
        gssCodes: List<String>,
        sourceType: SourceType,
        pageRequest: Pageable
    ): Page<AnonymousElectorDocumentSummary>

    fun findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
        gssCodes: List<String>,
        sourceType: SourceType,
        sanitizedSurname: String,
        pageRequest: Pageable
    ): Page<AnonymousElectorDocumentSummary>
}
