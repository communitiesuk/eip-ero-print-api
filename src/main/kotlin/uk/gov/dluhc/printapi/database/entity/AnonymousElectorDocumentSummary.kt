package uk.gov.dluhc.printapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.Type
import uk.gov.dluhc.printapi.database.repository.UUIDCharType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Id
import javax.persistence.Table

@Table(name = "v_anonymous_elector_document_summary")
@Entity
class AnonymousElectorDocumentSummary(

    @Id
    @Type(type = UUIDCharType)
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
    val postcode: String
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
