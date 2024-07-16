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
class AnonymousElectorDocument(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    var id: UUID? = null,

    @field:NotNull
    @field:Size(max = 20)
    var certificateNumber: String,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var sourceType: SourceType,

    @field:NotNull
    @field:Size(max = 255)
    var sourceReference: String,

    @field:NotNull
    @field:Size(max = 255)
    var applicationReference: String,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var certificateLanguage: CertificateLanguage,

    @Enumerated(EnumType.STRING)
    var supportingInformationFormat: SupportingInformationFormat?,

    @field:NotNull
    @field:Size(max = 1024)
    var photoLocationArn: String,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "aed_contact_details_id")
    var contactDetails: AedContactDetails?,

    @field:NotNull
    @field:Size(max = 80)
    var gssCode: String,

    @field:NotNull
    @field:Size(max = 30)
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

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    var delivery: Delivery? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "aed_id", nullable = false)
    var statusHistory: MutableList<AnonymousElectorDocumentStatus> = mutableListOf(),

    /**
     * The legislation stipulates there are two retention periods for AED related data. The first (initial)
     * period (which this field relates to) applies to PII data that is not on the printed document itself (e.g. the
     * addressee/address on the envelope). This needs to be removed 15 months after the AED is "issued" (generated).
     * The second (final) retention period is handled by the `finalRetentionRemovalDate` field below.
     */
    var initialRetentionRemovalDate: LocalDate? = null,

    /**
     * Set to true after the initial retention period data is removed.
     */
    var initialRetentionDataRemoved: Boolean = false,

    /**
     * The date that all remaining document data should be removed after the second (final) retention period. This is
     * currently specified as the tenth 1st July in the legislation.
     */
    var finalRetentionRemovalDate: LocalDate? = null,

    @CreationTimestamp
    @Column(updatable = false)
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long = 0L
) {

    fun addStatus(newStatus: AnonymousElectorDocumentStatus): AnonymousElectorDocument {
        statusHistory += newStatus
        return this
    }

    fun removeInitialRetentionPeriodData() {
        contactDetails?.email = null
        contactDetails?.phoneNumber = null
        contactDetails?.address = null
        delivery = null
        supportingInformationFormat = null
        initialRetentionDataRemoved = true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AnonymousElectorDocument

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , gssCode = $gssCode, dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}
