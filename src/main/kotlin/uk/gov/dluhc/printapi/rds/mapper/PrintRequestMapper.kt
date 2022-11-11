package uk.gov.dluhc.printapi.rds.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.service.IdFactory
import java.time.Clock
import java.time.Instant

@Mapper(uses = [InstantMapper::class, RdsElectoralRegistrationOfficeMapper::class])
abstract class PrintRequestMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var clock: Clock

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "requestId", expression = "java( idFactory.requestId() )")
    @Mapping(source = "message.photoLocation", target = "photoLocationArn")
    @Mapping(target = "statusHistory", expression = "java( initialStatus() )")
    @Mapping(source = "ero", target = "eroEnglish")
    @Mapping(source = "ero", target = "eroWelsh", conditionExpression = "java( isWelsh(message) )")
    abstract fun toPrintRequest(
        message: SendApplicationToPrintMessage,
        ero: EroManagementApiEroDto
    ): PrintRequest

    protected fun isWelsh(message: SendApplicationToPrintMessage): Boolean {
        return message.certificateLanguage == CertificateLanguage.CY
    }

    protected fun initialStatus(): List<PrintRequestStatus> {
        val now = Instant.now(clock)
        return listOf(
            PrintRequestStatus(
                status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                dateCreated = now,
                eventDateTime = now
            )
        )
    }
}
