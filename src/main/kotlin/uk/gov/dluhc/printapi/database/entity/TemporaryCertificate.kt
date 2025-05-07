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
import java.time.LocalDate
import java.util.UUID

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class TemporaryCertificate(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    var id: UUID? = null,

    @field:NotNull
    @field:Size(max = 20)
    var certificateNumber: String? = null,

    @field:NotNull
    @field:Size(max = 80)
    var gssCode: String? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var sourceType: SourceType? = null,

    @field:NotNull
    @field:Size(max = 255)
    var sourceReference: String? = null,

    @field:Size(max = 255)
    var applicationReference: String? = null,

    @field:NotNull
    @field:Size(max = 255)
    var certificateTemplateFilename: String? = null,

    @field:NotNull
    @field:Size(max = 255)
    var issuingAuthority: String? = null,

    @field:Size(max = 255)
    var issuingAuthorityCy: String? = null,

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
    @field:Size(max = 1024)
    var photoLocationArn: String? = null,

    @field:NotNull
    var issueDate: LocalDate = LocalDate.now(),

    @field:NotNull
    var validOnDate: LocalDate? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "temporary_certificate_id", nullable = false)
    var statusHistory: MutableList<TemporaryCertificateStatus> = mutableListOf(),

    /**
     * The date this [TemporaryCertificate] should be removed. This is specified as the second 1st July in the
     * legislation.
     */
    var finalRetentionRemovalDate: LocalDate? = null,

    @field:NotNull
    @field:Size(max = 255)
    var userId: String? = null,

    @CreationTimestamp
    @Column(updatable = false)
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long = 0L
) {

    fun getNameOnCertificate(): String {
        return if (middleNames.isNullOrBlank()) {
            "$firstName $surname"
        } else {
            "$firstName $middleNames $surname"
        }
    }

    fun addTemporaryCertificateStatus(status: TemporaryCertificateStatus): TemporaryCertificate {
        statusHistory += status
        return this
    }

    val status: TemporaryCertificateStatus.Status?
        get() = statusHistory.sortedByDescending { it.dateCreated }.first().status

    val dateTimeGenerated: Instant?
        get() = statusHistory
            .sortedBy { it.dateCreated }
            .first { it.status == TemporaryCertificateStatus.Status.GENERATED }.dateCreated

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as TemporaryCertificate

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , gssCode = $gssCode, dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}
