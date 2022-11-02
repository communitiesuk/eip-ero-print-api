package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import java.time.OffsetDateTime
import java.util.Date

@Mapper
abstract class PrintDetailsToPrintRequestMapper {

    @Mapping(source = "details.vacNumber", target = "cardNumber")
    @Mapping(source = "details.vacVersion", target = "cardVersion")
    @Mapping(source = "details.firstName", target = "cardFirstname")
    @Mapping(source = "details.middleNames", target = "cardMiddleNames")
    @Mapping(source = "details.surname", target = "cardSurname")
    @Mapping(source = "details.delivery.addressee", target = "deliveryName")
    @Mapping(source = "details.delivery.address.property", target = "deliveryProperty")
    @Mapping(source = "details.delivery.address.street", target = "deliveryStreet")
    @Mapping(source = "details.delivery.address.town", target = "deliveryTown")
    @Mapping(source = "details.delivery.address.locality", target = "deliveryLocality")
    @Mapping(source = "details.delivery.address.area", target = "deliveryArea")
    @Mapping(source = "details.delivery.address.postcode", target = "deliveryPostcode")
    @Mapping(source = "details.delivery.deliveryClass", target = "deliveryOption")
    @Mapping(source = "details.eroEnglish.name", target = "eroNameEn")
    @Mapping(source = "details.eroEnglish.phoneNumber", target = "eroPhoneNumberEn")
    @Mapping(source = "details.eroEnglish.website", target = "eroWebsiteEn")
    @Mapping(source = "details.eroEnglish.address.property", target = "eroDeliveryPropertyEn")
    @Mapping(source = "details.eroEnglish.address.street", target = "eroDeliveryStreetEn")
    @Mapping(source = "details.eroEnglish.address.town", target = "eroDeliveryTownEn")
    @Mapping(source = "details.eroEnglish.address.locality", target = "eroDeliveryLocalityEn")
    @Mapping(source = "details.eroEnglish.address.area", target = "eroDeliveryAreaEn")
    @Mapping(source = "details.eroEnglish.address.postcode", target = "eroDeliveryPostcodeEn")
    @Mapping(source = "details.eroEnglish.emailAddress", target = "eroEmailAddressEn")
    @Mapping(source = "details.issuingAuthority", target = "issuingAuthorityEn")
    @Mapping(source = "details.eroWelsh.name", target = "eroNameCy")
    @Mapping(source = "details.eroWelsh.phoneNumber", target = "eroPhoneNumberCy")
    @Mapping(source = "details.eroWelsh.website", target = "eroWebsiteCy")
    @Mapping(source = "details.eroWelsh.address.property", target = "eroDeliveryPropertyCy")
    @Mapping(source = "details.eroWelsh.address.street", target = "eroDeliveryStreetCy")
    @Mapping(source = "details.eroWelsh.address.town", target = "eroDeliveryTownCy")
    @Mapping(source = "details.eroWelsh.address.locality", target = "eroDeliveryLocalityCy")
    @Mapping(source = "details.eroWelsh.address.area", target = "eroDeliveryAreaCy")
    @Mapping(source = "details.eroWelsh.address.postcode", target = "eroDeliveryPostcodeCy")
    @Mapping(source = "details.eroWelsh.emailAddress", target = "eroEmailAddressCy")
    @Mapping(source = "details.eroWelsh.name", target = "issuingAuthorityCy")
    @Mapping(source = "details.requestDateTime", target = "requestDateTime")
    @Mapping(source = "photoZipPath", target = "photo")
    abstract fun map(details: PrintDetails, photoZipPath: String): PrintRequest

    fun map(offsetDateTime: OffsetDateTime): Date = Date.from(offsetDateTime.toInstant())
}
