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
class Certificate(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    var id: UUID? = null,

    @field:NotNull
    @field:Size(max = 20)
    var vacNumber: String? = null,

    @field:NotNull
    @Enumerated(EnumType.STRING)
    var sourceType: SourceType? = null,

    @field:NotNull
    @field:Size(max = 255)
    var sourceReference: String? = null,

    @field:Size(max = 255)
    var applicationReference: String? = null,

    @field:NotNull
    var applicationReceivedDateTime: Instant? = null,

    @field:NotNull
    @field:Size(max = 255)
    var issuingAuthority: String? = null,

    @field:Size(max = 255)
    var issuingAuthorityCy: String? = null,

    var issueDate: LocalDate? = null,

    /**
     * The certificate's expiry date. Not to be confused with removal dates related to data retention policies.
     */
    var suggestedExpiryDate: LocalDate? = null,

    /**
     * The legislation stipulates there are three retention periods for certificate related data. The first (initial)
     * period (which this field relates to), applies to PII data that is not on the printed certificate itself (e.g. the
     * addressee/address on the envelope). This needs to be removed 28 (configurable) working days after the
     * certificate is "issued".
     * The second retention period applies to Temporary Certificates (see [TemporaryCertificate]) and the third (final)
     * retention period is handled by `finalRetentionRemovalDate` below.
     */
    var initialRetentionRemovalDate: LocalDate? = null,

    /**
     * Set to true after the initial retention period data is removed.
     */
    var initialRetentionDataRemoved: Boolean = false,

    /**
     * The date that all remaining certificate data should be removed after the third (final) retention period. This is
     * currently specified as the tenth 1st July in the legislation.
     */
    var finalRetentionRemovalDate: LocalDate? = null,

    /**
     * Set to true when the certificate is associated with an application from the Applications API, rather than the
     * legacy Voter Card Applications API
     */
    var isFromApplicationsApi: Boolean? = false,

    /**
     * Set to true after the source application is removed.
     */
    var hasSourceApplicationBeenRemoved: Boolean? = false,

    /**
     * Temporary flag used to indicate whether the certificate was created after the rollout
     *
     * TODO EROPSPT-XXX: Remove this field once all certificates created before the rollout have been deleted
     */
    var isCertificateCreatedWithPrinterProvidedIssueDate: Boolean? = true,

    /**
     * Certificate status corresponds to the current status of the most recent
     * [PrintRequest], based on the requestDateTime that is included in the
     * [uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage].
     */
    @field:NotNull
    @Enumerated(EnumType.STRING)
    var status: PrintRequestStatus.Status? = null,

    @field:NotNull
    @field:Size(max = 80)
    var gssCode: String? = null,

    @field:NotNull
    @field:Size(max = 1024)
    var photoLocationArn: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_id", nullable = false)
    var printRequests: MutableList<PrintRequest> = mutableListOf(),

    @CreationTimestamp
    @Column(updatable = false)
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long = 0L
) {

    fun addPrintRequest(newPrintRequest: PrintRequest): Certificate {
        printRequests += newPrintRequest
        assignStatus()
        return this
    }

    fun getPrintRequestsByStatusAndBatchId(printRequestStatus: PrintRequestStatus.Status, batchId: String) =
        printRequests.filter { it.getCurrentStatus().status == printRequestStatus && it.batchId == batchId }

    fun addPrintRequestToBatch(printRequest: PrintRequest, batchId: String) {
        processPrintRequestUpdate {
            printRequest.addPrintRequestStatus(
                PrintRequestStatus(
                    status = PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
                    eventDateTime = Instant.now(),
                    message = null
                )
            )
            printRequest.batchId = batchId
        }
    }

    fun addSentToPrintProviderEventForBatch(batchId: String) {
        processPrintRequestUpdate {
            getPrintRequestsByBatchId(batchId).forEach {
                it.addPrintRequestStatus(
                    PrintRequestStatus(
                        status = PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now(),
                        message = null
                    )
                )
            }
        }
    }

    fun addReceivedByPrintProviderEventForBatch(
        batchId: String,
        eventDateTime: Instant,
        message: String?
    ) {
        processPrintRequestUpdate {
            getPrintRequestsByBatchId(batchId).forEach {
                it.addPrintRequestStatus(
                    PrintRequestStatus(
                        status = PrintRequestStatus.Status.RECEIVED_BY_PRINT_PROVIDER,
                        eventDateTime = eventDateTime,
                        message = message
                    )
                )
            }
        }
    }

    fun requeuePrintRequestForBatch(
        batchId: String,
        eventDateTime: Instant,
        message: String?,
        newRequestId: String
    ) {
        processPrintRequestUpdate {
            getPrintRequestsByBatchId(batchId).forEach {
                it.addPrintRequestStatus(
                    PrintRequestStatus(
                        status = PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = eventDateTime,
                        message = message
                    )
                )
                it.batchId = null
                it.requestId = newRequestId
            }
        }
    }

    fun addPrintRequestEvent(
        requestId: String,
        status: PrintRequestStatus.Status,
        eventDateTime: Instant,
        message: String?
    ) {
        processPrintRequestUpdate {
            getPrintRequestsByRequestId(requestId).forEach {
                it.addPrintRequestStatus(
                    PrintRequestStatus(
                        status = status,
                        eventDateTime = eventDateTime,
                        message = message
                    )
                )
            }
        }
    }

    private fun processPrintRequestUpdate(update: () -> Unit) {
        update.invoke()
        assignStatus()
    }

    private fun getCurrentPrintRequest(): PrintRequest = printRequests.sortedByDescending { it.requestDateTime }.first()

    private fun getPrintRequestsByRequestId(requestId: String) = printRequests.filter { it.requestId == requestId }

    private fun getPrintRequestsByBatchId(batchId: String) = printRequests.filter { it.batchId == batchId }

    private fun assignStatus() {
        val currentPrintRequest = getCurrentPrintRequest()
        val currentStatus = currentPrintRequest.getCurrentStatus()
        status = currentStatus.status
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
        return this::class.simpleName + "(id = $id , gssCode = $gssCode, dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}
