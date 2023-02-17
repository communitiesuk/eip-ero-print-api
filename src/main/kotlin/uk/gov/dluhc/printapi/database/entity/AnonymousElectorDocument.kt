package uk.gov.dluhc.printapi.database.entity

import liquibase.pro.packaged.it
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
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.persistence.Version
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class AnonymousElectorDocument(

    @Id
    @Type(type = UUIDCharType)
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = UseExistingOrGenerateUUID.NAME)
    var id: UUID? = null,

    @field:NotNull
    @field:Size(max = 20)
    var certificateNumber: String? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var sourceType: SourceType? = null,

    @field:NotNull
    @field:Size(max = 255)
    var sourceReference: String? = null,

    @field:Size(max = 255)
    var applicationReference: String? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var certificateLanguage: CertificateLanguage? = null,

    @Enumerated(EnumType.STRING)
    var supportingInformationFormat: SupportingInformationFormat? = null,

    @field:NotNull
    @field:Size(max = 255)
    var photoLocationArn: String? = null,

    @field:NotNull
    @field:Size(max = 255)
    var firstName: String? = null,

    @field:Size(max = 255)
    var middleNames: String? = null,

    @field:NotNull
    @field:Size(max = 255)
    var surname: String? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    var address: Address,

    @field:NotNull
    @field:Size(max = 80)
    var gssCode: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "aed_id", nullable = false)
    var printRequests: MutableList<AEDPrintRequest> = mutableListOf(),

    /**
     * The legislation stipulates there are two retention periods for AED related data. The first (initial)
     * period applies to PII data that is not on the printed certificate itself (e.g. the addressee/address on the
     * envelope), which needs to be removed 15 months after the AED is "issued".
     * For standard (non-temporary) certificates, the retention period is considerably longer and is currently specified
     * as the tenth 1st July.
     */
    var initialRetentionRemovalDate: LocalDate? = null,

    @CreationTimestamp
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long? = null
) {

    val status: AEDPrintRequestStatus.Status?
        get() = printRequests.sortedByDescending { it.requestDateTime }.first().status

    fun addPrintRequest(newPrintRequest: AEDPrintRequest): AnonymousElectorDocument {
        printRequests += newPrintRequest
        return this
    }

    private fun getCurrentPrintRequest(): AEDPrintRequest = printRequests.sortedByDescending { it.requestDateTime }.first()

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
