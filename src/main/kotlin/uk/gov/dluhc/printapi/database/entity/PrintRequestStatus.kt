package uk.gov.dluhc.printapi.database.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Types
import java.time.Instant
import java.util.UUID

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class PrintRequestStatus(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    var id: UUID? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var status: Status? = null,

    @field:NotNull
    var eventDateTime: Instant? = null, // either the "timestamp" from the print provider, or the current time

    @field:Size(max = 1024)
    var message: String? = null,

    @CreationTimestamp
    @Column(updatable = false)
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long = 0L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PrintRequestStatus

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , status = $status , dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }

    enum class Status {
        PENDING_ASSIGNMENT_TO_BATCH,
        ASSIGNED_TO_BATCH,
        SENT_TO_PRINT_PROVIDER,
        RECEIVED_BY_PRINT_PROVIDER,
        VALIDATED_BY_PRINT_PROVIDER,
        IN_PRODUCTION,
        PRINTED,
        DISPATCHED,
        NOT_DELIVERED,
        PRINT_PROVIDER_VALIDATION_FAILED,
        PRINT_PROVIDER_PRODUCTION_FAILED, // TODO EROPSPT-418 this is never used
        PRINT_PROVIDER_DISPATCH_FAILED, // TODO EROPSPT-418 this is never used
    }
}
