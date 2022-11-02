package uk.gov.dluhc.printapi.service

import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.domain.CertificateNumber
import java.util.UUID

/**
 * Simple factory bean for creating different types of IDs and reference numbers.
 */
@Component
class IdFactory {
    fun requestId(): String = ObjectId().toString()

    fun vacNumber(): String = CertificateNumber.create()

    fun batchId(): String = UUID.randomUUID().toString().replace("-", "")
}
