package uk.gov.dluhc.printapi.mapper

import org.apache.commons.lang3.RandomStringUtils
import org.bson.types.ObjectId
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import java.util.UUID

@Mapper(uses = [SourceTypeMapper::class], imports = [UUID::class, ObjectId::class, RandomStringUtils::class])
abstract class PrintDetailsMapper {
    @Mapping(target = "id", expression = "java(UUID.randomUUID())")
    @Mapping(target = "requestId", expression = "java( new ObjectId().toHexString())")
    @Mapping(target = "vacNumber", expression = "java( RandomStringUtils.randomAlphanumeric(20))")
    @Mapping(source = "ero", target = "eroEnglish")
    @Mapping(source = "ero", target = "eroWelsh", conditionExpression = "java(isWelsh(message))")
    @Mapping(source = "localAuthority", target = "issuingAuthority")
    abstract fun toPrintDetails(
        message: SendApplicationToPrintMessage,
        ero: EroManagementApiEroDto,
        localAuthority: String
    ): PrintDetails

    fun isWelsh(message: SendApplicationToPrintMessage): Boolean {
        return message.certificateLanguage == CertificateLanguage.CY
    }
}
