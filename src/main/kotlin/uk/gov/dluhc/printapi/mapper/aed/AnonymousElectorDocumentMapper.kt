package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.dto.CertificateDelivery
import uk.gov.dluhc.printapi.dto.aed.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.CertificateLanguageMapper
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.models.GenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.service.IdFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Mapper(
    uses = [
        AnonymousSupportingInformationFormatMapper::class,
        CertificateLanguageMapper::class,
        DeliveryAddressTypeMapper::class,
        SourceTypeMapper::class,
    ]
)
abstract class AnonymousElectorDocumentMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var clock: Clock

    abstract fun toGenerateAnonymousElectorDocumentDto(
        apiRequest: GenerateAnonymousElectorDocumentRequest,
        userId: String
    ): GenerateAnonymousElectorDocumentDto

    @Mapping(target = "photoLocationArn", source = "aedDto.photoLocation")
    @Mapping(target = "certificateNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(target = "issueDate", expression = "java( issueDate() )")
    @Mapping(target = "requestDateTime", expression = "java( requestDateTime() )")
    @Mapping(target = "contactDetails", source = "aedDto")
    @Mapping(target = "statusHistory", expression = "java( markStatusAsPrinted() )")
    @Mapping(target = "delivery", source = "aedDto.delivery")
    abstract fun toAnonymousElectorDocument(
        aedDto: GenerateAnonymousElectorDocumentDto,
        aedTemplateFilename: String
    ): AnonymousElectorDocument

    @Mapping(target = "address", source = "registeredAddress")
    protected abstract fun toAedContactDetailsEntity(aedDto: GenerateAnonymousElectorDocumentDto): AedContactDetails

    @Mapping(target = "address", source = "deliveryAddress")
    protected abstract fun fromDeliveryDtoToDeliveryEntity(deliveryDto: CertificateDelivery): Delivery

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
