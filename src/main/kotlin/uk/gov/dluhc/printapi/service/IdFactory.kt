package uk.gov.dluhc.printapi.service

import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Simple factory bean for creating different types of IDs and reference numbers.
 */
@Component
class IdFactory {
    fun requestId(): String = ObjectId().toString()

    fun vacNumber(): String = randomAlphanumeric(20)

    fun batchId(): String = UUID.randomUUID().toString().replace("-", "")
}
