package uk.gov.dluhc.printapi.database.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table(name = "v_anonymous_elector_document_summary")
@Entity
class AnonymousElectorDocumentSummary(

    @Id
    @JdbcTypeCode(Types.CHAR)
    val id: UUID,

    val gssCode: String,

    @Enumerated(EnumType.STRING)
    val sourceType: SourceType,

    val electoralRollNumber: String,

    val sanitizedElectoralRollNumber: String,

    val certificateNumber: String,

    val sourceReference: String,

    val applicationReference: String,

    val issueDate: LocalDate,

    val dateCreated: Instant,

    val firstName: String,

    val surname: String,

    val sanitizedSurname: String,

    val postcode: String?,

    val initialRetentionDataRemoved: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AnonymousElectorDocumentSummary

        return id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id, gssCode = $gssCode, sourceReference = $sourceReference, applicationReference = $applicationReference)"
    }
}
