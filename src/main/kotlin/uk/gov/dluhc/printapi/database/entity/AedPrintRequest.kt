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
import java.time.LocalDate
import java.util.UUID
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EntityListeners
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
class AedPrintRequest(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @field:NotNull
    @field:Size(max = 7)
    var electoralRollNumber: String,

    @field:NotNull
    @field:Size(max = 255)
    var aedTemplateFilename: String,

    @field:NotNull
    var issueDate: LocalDate,

    @field:NotNull
    var requestDateTime: Instant,

    @field:NotNull
    @field:Size(max = 255)
    var userId: String,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "print_request_id", nullable = false)
    var statusHistory: MutableList<AedPrintRequestStatus> = mutableListOf(),

    @CreationTimestamp
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long? = null

) {

    fun addPrintRequestStatus(newPrintRequestStatus: AedPrintRequestStatus): AedPrintRequest {
        statusHistory += newPrintRequestStatus
        return this
    }

    val status: AedPrintRequestStatus.Status?
        get() = statusHistory.sortedByDescending { it.eventDateTime }.first().status

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AedPrintRequest

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}