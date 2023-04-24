package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.database.entity.SourceType
import java.util.UUID

@Repository
interface AnonymousElectorDocumentSummaryRepository :
    PagingAndSortingRepository<AnonymousElectorDocumentSummary, UUID> {

    fun findByGssCodeInAndSourceTypeOrderByIssueDateDescSanitizedSurnameAsc(
        gssCodes: List<String>,
        sourceType: SourceType,
    ): List<AnonymousElectorDocumentSummary>
}
