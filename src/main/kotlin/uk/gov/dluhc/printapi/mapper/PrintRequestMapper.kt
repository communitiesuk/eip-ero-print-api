package uk.gov.dluhc.printapi.mapper

import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.service.IdFactory
import java.time.Clock
import java.time.Instant

@Mapper(
    uses = [
        InstantMapper::class,
        SupportingInformationFormatMapper::class,
        DeliveryAddressTypeMapper::class,
    ]
)
abstract class PrintRequestMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var clock: Clock

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vacVersion", constant = "A")
    @Mapping(target = "requestId", expression = "java( idFactory.requestId() )")
    @Mapping(source = "message.photoLocation", target = "photoLocationArn")
    @Mapping(target = "statusHistory", expression = "java( initialStatus() )")
    @Mapping(source = "ero.englishContactDetails", target = "eroEnglish")
    @Mapping(constant = "Electoral Registration Officer", target = "eroEnglish.name")
    @Mapping(source = "ero.welshContactDetails", target = "eroWelsh")
    @Mapping(constant = "Swyddog Cofrestru Etholiadol", target = "eroWelsh.name")
    abstract fun toPrintRequest(
        message: SendApplicationToPrintMessage,
        ero: EroDto,
    ): PrintRequest

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
