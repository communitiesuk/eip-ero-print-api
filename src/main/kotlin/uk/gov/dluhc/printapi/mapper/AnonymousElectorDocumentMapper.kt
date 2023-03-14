package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.service.IdFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Mapper(
    uses = [
        SourceTypeMapper::class,
        CertificateLanguageMapper::class,
        SupportingInformationFormatMapper::class
    ]
)
abstract class AnonymousElectorDocumentMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var clock: Clock

    @Mapping(target = "photoLocationArn", source = "aedRequest.photoLocation")
    @Mapping(target = "certificateNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(target = "issueDate", expression = "java( issueDate() )")
    @Mapping(target = "requestDateTime", expression = "java( requestDateTime() )")
    @Mapping(target = "contactDetails", source = "aedRequest")
    @Mapping(target = "statusHistory", expression = "java( markStatusAsPrinted() )")
    abstract fun toAnonymousElectorDocument(
        aedRequest: GenerateAnonymousElectorDocumentDto,
        aedTemplateFilename: String
    ): AnonymousElectorDocument

    protected fun issueDate(): LocalDate = LocalDate.now(clock)

    protected fun requestDateTime(): Instant = Instant.now(clock)

    protected fun markStatusAsPrinted(): List<AnonymousElectorDocumentStatus> {
        return listOf(
            AnonymousElectorDocumentStatus(
                status = AnonymousElectorDocumentStatus.Status.PRINTED,
                eventDateTime = Instant.now(clock)
            )
        )
    }
}
