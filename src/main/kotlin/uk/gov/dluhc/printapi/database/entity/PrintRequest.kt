package uk.gov.dluhc.printapi.database.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.JdbcTypeCode
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Types
import java.time.Instant
import java.util.UUID

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class PrintRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
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

    @Column(nullable = true, insertable = false, updatable = false)
    var sanitizedSurname: String? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var certificateLanguage: CertificateLanguage? = null,

    @Enumerated(EnumType.STRING)
    var supportingInformationFormat: SupportingInformationFormat? = null,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
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
    var version: Long = 0L

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
