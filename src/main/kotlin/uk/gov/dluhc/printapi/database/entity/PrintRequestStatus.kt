package uk.gov.dluhc.printapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import uk.gov.dluhc.printapi.database.repository.UUIDCharType
import uk.gov.dluhc.printapi.database.repository.UseExistingOrGenerateUUID
import java.time.Instant
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class PrintRequestStatus(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var status: Status? = null,

    @field:NotNull
    var eventDateTime: Instant? = null, // either the "timestamp" from the print provider, or the current time

    @field:Size(max = 1024)
    var message: String? = null,

    @CreationTimestamp
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long? = null
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
        DISPATCHED,
        NOT_DELIVERED,
        PRINT_PROVIDER_VALIDATION_FAILED,
        PRINT_PROVIDER_PRODUCTION_FAILED,
        PRINT_PROVIDER_DISPATCH_FAILED,
    }
}
