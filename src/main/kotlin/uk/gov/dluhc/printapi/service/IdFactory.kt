package uk.gov.dluhc.printapi.service

import org.bson.types.ObjectId
import org.springframework.stereotype.Component

/**
 * Simple factory bean for creating different types of IDs and reference numbers.
 */
@Component
class IdFactory(private val certificateNumberGenerator: CertificateNumberGenerator) {
    fun requestId(): String = ObjectId().toString()

    fun vacNumber(): String = certificateNumberGenerator.generateCertificateNumber()
}
