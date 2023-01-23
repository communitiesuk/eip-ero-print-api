package uk.gov.dluhc.printapi.database.entity

import org.hibernate.Hibernate
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.Type
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import uk.gov.dluhc.printapi.database.repository.UUIDCharType
import uk.gov.dluhc.printapi.database.repository.UseExistingOrGenerateUUID
import java.time.Instant
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
import javax.persistence.OneToOne
import javax.persistence.PrePersist
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class PrintRequest(
    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @field:NotNull
    @field:Size(max = 24)
    var requestId: String? = null,

    @field:NotNull
    @field:Size(max = 20)
    var vacVersion: String? = null,

    @field:NotNull
    var requestDateTime: Instant? = null,

    @field:NotNull
    @field:Size(max = 255)
    var firstName: String? = null,

    @field:Size(max = 255)
    var middleNames: String? = null,

    @field:NotNull
    @field:Size(max = 255)
    var surname: String? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var certificateLanguage: CertificateLanguage? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var supportingInformationFormat: SupportingInformationFormat? = null,

    @field:NotNull
    @field:Size(max = 255)
    var photoLocationArn: String? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    var delivery: Delivery? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "english_ero_id")
    var eroEnglish: ElectoralRegistrationOffice? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "welsh_ero_id")
    var eroWelsh: ElectoralRegistrationOffice? = null,

    @field:NotNull
    @field:Size(max = 255)
    var userId: String? = null,

    @field:Size(max = 255)
    var batchId: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "print_request_id", nullable = false)
    var statusHistory: MutableList<PrintRequestStatus> = mutableListOf(),

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

    fun addPrintRequestStatus(newPrintRequestStatus: PrintRequestStatus): PrintRequest {
        statusHistory += newPrintRequestStatus
        return this
    }

    fun getCurrentStatus(): PrintRequestStatus {
        statusHistory.sortByDescending { it.eventDateTime }
        return statusHistory.first()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PrintRequest

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , batchId = $batchId , dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}
