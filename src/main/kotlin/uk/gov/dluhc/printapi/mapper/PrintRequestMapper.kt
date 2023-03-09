package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
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

    @Autowired
    protected lateinit var electoralRegistrationOfficeMapper: ElectoralRegistrationOfficeMapper

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vacVersion", constant = "A")
    @Mapping(target = "requestId", expression = "java( idFactory.requestId() )")
    @Mapping(target = "statusHistory", expression = "java( initialStatus() )")
    @Mapping(target = "eroEnglish", expression = "java( toEnglishContactDetails(ero) )")
    @Mapping(target = "eroWelsh", expression = "java( toWelshContactDetails(message, ero) )")
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

    protected fun toEnglishContactDetails(ero: EroDto): ElectoralRegistrationOffice? =
        electoralRegistrationOfficeMapper
            .toElectoralRegistrationOffice(ero.englishContactDetails, CertificateLanguage.EN)

    protected fun toWelshContactDetails(
        message: SendApplicationToPrintMessage,
        ero: EroDto
    ): ElectoralRegistrationOffice? {
        val shouldPopulateFromEnglishContactDetails =
            message.certificateLanguage == CertificateLanguage.CY && ero.welshContactDetails == null

        if (shouldPopulateFromEnglishContactDetails) {
            return electoralRegistrationOfficeMapper
                .toElectoralRegistrationOffice(ero.englishContactDetails, CertificateLanguage.EN)
        }

        return electoralRegistrationOfficeMapper
            .toElectoralRegistrationOffice(ero.welshContactDetails, CertificateLanguage.CY)
    }
}
