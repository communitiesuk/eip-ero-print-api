package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Context
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto
import uk.gov.dluhc.printapi.dto.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.factory.UrlFactory
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse

@Mapper(
    uses = [
        PrintRequestStatusMapper::class,
        InstantMapper::class,
        DeliveryAddressTypeMapper::class
    ]
)
abstract class CertificateSummaryResponseMapper {

    @Autowired
    protected lateinit var urlFactory: UrlFactory

    @Mapping(target = "printRequestSummaries", source = "dto.printRequests")
    @Mapping(target = "photoUrl", expression = "java(getPhotoUrl(eroId, dto))")
    abstract fun toCertificateSummaryResponse(
        dto: CertificateSummaryDto,
        @Context eroId: String
    ): CertificateSummaryResponse

    protected fun getPhotoUrl(eroId: String, dto: CertificateSummaryDto): String =
        urlFactory.createPhotoUrl(eroId, VOTER_CARD, dto.sourceReference)
}
