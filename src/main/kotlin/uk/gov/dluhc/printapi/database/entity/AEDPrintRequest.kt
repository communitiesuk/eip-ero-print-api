package uk.gov.dluhc.printapi.database.entity

import org.hibernate.Hibernate
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
import javax.persistence.PrePersist
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Table(name = "aed_print_request")
@Entity
@EntityListeners(AuditingEntityListener::class)
class AEDPrintRequest(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @field:NotNull
    @field:Size(max = 20)
    var electoralNumber: String? = null,

    @field:NotNull
    @field:Size(max = 255)
    var aedTemplateFilename: String? = null,

    @field:NotNull
    var issueDate: LocalDate = LocalDate.now(),

    @field:NotNull
    var requestDateTime: Instant? = null,

    @field:NotNull
    @field:Size(max = 255)
    var userId: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "print_request_id", nullable = false)
    var statusHistory: MutableList<AEDPrintRequestStatus> = mutableListOf(),

    // managed using JPA pre-persist annotated method below
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long? = null

) {

    @PrePersist
    fun populateDateCreated() {
        if (dateCreated == null) {
            dateCreated = Instant.now()
        }
    }

    fun addPrintRequestStatus(newPrintRequestStatus: AEDPrintRequestStatus): AEDPrintRequest {
        statusHistory += newPrintRequestStatus
        return this
    }

    val status: AEDPrintRequestStatus.Status?
        get() = statusHistory.sortedByDescending { it.dateCreated }.first().status

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AEDPrintRequest

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}
