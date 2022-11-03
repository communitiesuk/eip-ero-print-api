package uk.gov.dluhc.printapi.mapper

import org.bson.types.ObjectId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.service.IdFactory
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Mapper(
    uses = [SourceTypeMapper::class],
    imports = [UUID::class, ObjectId::class]
)
abstract class PrintDetailsMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var clock: Clock

    @Mapping(target = "id", expression = "java( UUID.randomUUID() )")
    @Mapping(target = "requestId", expression = "java( idFactory.requestId() )")
    @Mapping(target = "vacNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(target = "printRequestStatuses", expression = "java( initialStatus() )")
    @Mapping(source = "ero", target = "eroEnglish")
    @Mapping(source = "ero", target = "eroWelsh", conditionExpression = "java( isWelsh(message) )")
    @Mapping(source = "localAuthority", target = "issuingAuthority")
    abstract fun toPrintDetails(
        message: SendApplicationToPrintMessage,
        ero: EroManagementApiEroDto,
        localAuthority: String
    ): PrintDetails

    protected fun isWelsh(message: SendApplicationToPrintMessage): Boolean {
        return message.certificateLanguage == CertificateLanguage.CY
    }

    protected fun initialStatus(): List<PrintRequestStatus> =
        listOf(
            PrintRequestStatus(
                status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                dateTime = OffsetDateTime.now(clock)
            )
        )
}
