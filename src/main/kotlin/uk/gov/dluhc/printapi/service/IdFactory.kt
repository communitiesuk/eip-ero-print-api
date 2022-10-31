package uk.gov.dluhc.printapi.service

import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.domain.CertificateNumber

/**
 * Simple factory bean for creating different types of IDs and reference numbers.
 */
@Component
class IdFactory {
    fun requestId(): String = ObjectId().toString()

    fun vacNumber(): String = CertificateNumber().toString()
}
