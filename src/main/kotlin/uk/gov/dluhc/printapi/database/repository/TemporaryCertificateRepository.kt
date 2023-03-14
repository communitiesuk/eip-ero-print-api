package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import java.time.LocalDate
import java.util.UUID

@Repository
interface TemporaryCertificateRepository : JpaRepository<TemporaryCertificate, UUID> {

    fun findByGssCodeInAndSourceTypeAndSourceReference(gssCodes: List<String>, sourceType: SourceType, sourceReference: String): List<TemporaryCertificate>

    fun findBySourceTypeAndFinalRetentionRemovalDateBefore(sourceType: SourceType, finalRetentionRemovalDate: LocalDate): List<TemporaryCertificate>
}

object TemporaryCertificateRepositoryExtensions {

    fun TemporaryCertificateRepository.findPendingRemoval(sourceType: SourceType): List<TemporaryCertificate> {
        return findBySourceTypeAndFinalRetentionRemovalDateBefore(
            sourceType = sourceType,
            finalRetentionRemovalDate = LocalDate.now()
        )
    }
}
