package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AedPrintRequest
import uk.gov.dluhc.printapi.database.entity.AedPrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.AedPrintRequestStatus.Status
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Mapper
abstract class AedPrintRequestMapper {

    @Autowired
    protected lateinit var clock: Clock

    @Mapping(target = "issueDate", expression = "java( issueDate() )")
    @Mapping(target = "requestDateTime", expression = "java( requestDateTime() )")
    @Mapping(target = "statusHistory", expression = "java( initialStatus() )")
    abstract fun toPrintRequest(
        electoralDocumentRequest: GenerateAnonymousElectorDocumentDto,
        aedTemplateFilename: String
    ): AedPrintRequest

    protected fun issueDate(): LocalDate = LocalDate.now(clock)

    protected fun requestDateTime(): Instant = Instant.now(clock)

    protected fun initialStatus(): List<AedPrintRequestStatus> {
        return listOf(
            AedPrintRequestStatus(
                status = Status.GENERATED,
                eventDateTime = Instant.now(clock)
            )
        )
    }
}
