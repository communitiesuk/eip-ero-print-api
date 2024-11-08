package uk.gov.dluhc.printapi.database.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import jakarta.validation.constraints.Size
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Types
import java.time.Instant
import java.util.UUID

@Table
@Entity
@EntityListeners(AuditingEntityListener::class)
class Delivery(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.CHAR)
    var id: UUID? = null,

    @field:Size(max = 255)
    var addressee: String,

    @OneToOne(cascade = [CascadeType.ALL], orphanRemoval = true)
    var address: Address,

    @Enumerated(EnumType.STRING)
    var addressFormat: AddressFormat,

    @Enumerated(EnumType.STRING)
    var deliveryClass: DeliveryClass,

    @Enumerated(EnumType.STRING)
    var deliveryAddressType: DeliveryAddressType,

    @field:Size(max = 1024)
    var collectionReason: String? = null,

    @CreationTimestamp
    @Column(updatable = false)
    var dateCreated: Instant? = null,

    @field:Size(max = 255)
    @LastModifiedBy
    var createdBy: String? = null,

    @Version
    var version: Long = 0L

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Delivery

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , dateCreated = $dateCreated , createdBy = $createdBy , version = $version )"
    }
}
