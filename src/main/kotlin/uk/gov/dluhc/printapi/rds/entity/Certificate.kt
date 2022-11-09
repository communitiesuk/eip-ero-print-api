package uk.gov.dluhc.printapi.rds.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.rds.repository.UUIDCharType
import uk.gov.dluhc.printapi.rds.repository.UseExistingOrGenerateUUID
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class Certificate(

    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @NotNull
    @Size(max = 20)
    var vacNumber: String? = null,

    @NotNull
    @Size(max = 20)
    @Enumerated(EnumType.STRING)
    var sourceType: SourceType? = null,

    @NotNull
    @Size(max = 255)
    var sourceReference: String? = null,

    @Size(max = 255)
    var applicationReference: String? = null,

    @NotNull
    var applicationReceivedDateTime: OffsetDateTime? = null,

    @NotNull
    @Size(max = 255)
    var issuingAuthority: String? = null,

    @NotNull
    var issueDate: LocalDate = LocalDate.now(),

    @NotNull
    var suggestedExpiryDate: LocalDate = issueDate.plusYears(10),

    @NotNull
    @Size(max = 50)
    @Enumerated(EnumType.STRING)
    var status: Status = Status.PENDING_ASSIGNMENT_TO_BATCH,

    @NotNull
    @Size(max = 80)
    var gssCode: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    var printRequests: MutableList<PrintRequest> = mutableListOf(),

    @NotNull
    @UpdateTimestamp
    var dateCreated: Instant? = null,

    @NotNull
    @Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long? = null
) {

    fun addPrintRequest(newPrintRequest: PrintRequest): Certificate {
        printRequests += newPrintRequest
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Certificate

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , certificateNumber = $vacNumber , gssCode = $gssCode, dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}
