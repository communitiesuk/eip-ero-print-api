package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary
import java.util.UUID

@Repository
interface AnonymousElectorDocumentSummaryRepository :
    PagingAndSortingRepository<AnonymousElectorDocumentSummary, UUID>,
    JpaSpecificationExecutor<AnonymousElectorDocumentSummary>,
    JpaRepository<AnonymousElectorDocumentSummary, UUID>
