package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.SourceType
import java.time.LocalDate
import java.util.UUID

@Repository
interface AnonymousElectorDocumentRepository : JpaRepository<AnonymousElectorDocument, UUID> {

    fun findByGssCodeAndSourceTypeAndSourceReference(gssCode: String, sourceType: SourceType, sourceReference: String): List<AnonymousElectorDocument>

    fun findByGssCodeInAndSourceTypeAndSourceReferenceOrderByDateCreatedDesc(gssCodes: List<String>, sourceType: SourceType, sourceReference: String): List<AnonymousElectorDocument>

    fun findBySourceTypeAndInitialRetentionDataRemovedAndInitialRetentionRemovalDateBefore(
        sourceType: SourceType,
        initialRetentionDataRemoved: Boolean = false,
        initialRetentionRemovalDate: LocalDate
    ): List<AnonymousElectorDocument>

    fun findBySourceTypeAndFinalRetentionRemovalDateBefore(
        sourceType: SourceType,
        finalRetentionRemovalDate: LocalDate
    ): List<AnonymousElectorDocument>

    fun existsByPhotoLocationArnEqualsAndFinalRetentionRemovalDateGreaterThanEqual(
        photoLocationArn: String,
        finalRetentionRemovalDate: LocalDate
    ): Boolean
}

object AnonymousElectorDocumentRepositoryExtensions {

    fun AnonymousElectorDocumentRepository.findPendingRemovalOfInitialRetentionData(sourceType: SourceType): List<AnonymousElectorDocument> {
        return findBySourceTypeAndInitialRetentionDataRemovedAndInitialRetentionRemovalDateBefore(
            sourceType = sourceType,
            initialRetentionRemovalDate = LocalDate.now()
        )
    }

    fun AnonymousElectorDocumentRepository.findPendingRemovalOfFinalRetentionData(sourceType: SourceType): List<AnonymousElectorDocument> {
        return findBySourceTypeAndFinalRetentionRemovalDateBefore(
            sourceType = sourceType,
            finalRetentionRemovalDate = LocalDate.now()
        )
    }

    fun AnonymousElectorDocumentRepository.shouldRetainPhoto(photoLocationArn: String): Boolean {
        return existsByPhotoLocationArnEqualsAndFinalRetentionRemovalDateGreaterThanEqual(
            photoLocationArn = photoLocationArn,
            finalRetentionRemovalDate = LocalDate.now()
        )
    }
}
