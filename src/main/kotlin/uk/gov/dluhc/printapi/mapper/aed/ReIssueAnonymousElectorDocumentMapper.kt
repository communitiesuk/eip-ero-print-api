package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.aed.ReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.models.ReIssueAnonymousElectorDocumentRequest

@Mapper(
    uses = [
        DeliveryAddressTypeMapper::class,
    ]
)
interface ReIssueAnonymousElectorDocumentMapper {

    fun toReIssueAnonymousElectorDocumentDto(
        apiRequest: ReIssueAnonymousElectorDocumentRequest,
        userId: String
    ): ReIssueAnonymousElectorDocumentDto
}
