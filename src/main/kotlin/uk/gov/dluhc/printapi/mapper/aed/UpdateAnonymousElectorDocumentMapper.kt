package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.aed.UpdateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.models.UpdateAnonymousElectorDocumentRequest

@Mapper
interface UpdateAnonymousElectorDocumentMapper {

    fun toUpdateAedDto(updateAedRequest: UpdateAnonymousElectorDocumentRequest): UpdateAnonymousElectorDocumentDto
}
