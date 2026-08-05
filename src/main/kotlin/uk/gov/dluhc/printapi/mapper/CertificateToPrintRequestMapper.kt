package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest

@Mapper(
    uses = [
        InstantMapper::class,
        SupportingInformationFormatMapper::class,
        CertificateLanguageMapper::class
    ]
)
abstract class CertificateToPrintRequestMapper {

    @Mapping(source = "certificate.vacNumber", target = "cardNumber")
    @Mapping(source = "certificate.issuingAuthority", target = "issuingAuthorityEn")
    @Mapping(source = "certificate.issueDate", target = "issueDate")
    @Mapping(source = "certificate.suggestedExpiryDate", target = "suggestedExpiryDate")
    @Mapping(source = "printRequest.vacVersion", target = "cardVersion")
    @Mapping(source = "printRequest.firstName", target = "cardFirstname")
    @Mapping(source = "printRequest.middleNames", target = "cardMiddleNames")
    @Mapping(source = "printRequest.surname", target = "cardSurname")
    @Mapping(source = "printRequest.delivery.addressee", target = "deliveryName")
    @Mapping(source = "printRequest.delivery.address.property", target = "deliveryProperty")
    @Mapping(source = "printRequest.delivery.address.street", target = "deliveryStreet")
    @Mapping(source = "printRequest.delivery.address.town", target = "deliveryTown")
    @Mapping(source = "printRequest.delivery.address.locality", target = "deliveryLocality")
    @Mapping(source = "printRequest.delivery.address.area", target = "deliveryArea")
    @Mapping(source = "printRequest.delivery.address.postcode", target = "deliveryPostcode")
    @Mapping(source = "printRequest.certificateLanguage", target = "certificateLanguage")
    @Mapping(source = "printRequest.supportingInformationFormat", target = "certificateFormat")
    @Mapping(source = "printRequest.delivery.deliveryClass", target = "deliveryOption")
    @Mapping(source = "printRequest.eroEnglish.name", target = "eroNameEn")
    @Mapping(source = "printRequest.eroEnglish.phoneNumber", target = "eroPhoneNumberEn")
    @Mapping(source = "printRequest.eroEnglish.website", target = "eroWebsiteEn")
    @Mapping(source = "printRequest.eroEnglish.address.property", target = "eroDeliveryPropertyEn")
    @Mapping(source = "printRequest.eroEnglish.address.street", target = "eroDeliveryStreetEn")
    @Mapping(source = "printRequest.eroEnglish.address.town", target = "eroDeliveryTownEn")
    @Mapping(source = "printRequest.eroEnglish.address.locality", target = "eroDeliveryLocalityEn")
    @Mapping(source = "printRequest.eroEnglish.address.area", target = "eroDeliveryAreaEn")
    @Mapping(source = "printRequest.eroEnglish.address.postcode", target = "eroDeliveryPostcodeEn")
    @Mapping(source = "printRequest.eroEnglish.emailAddress", target = "eroEmailAddressEn")
    @Mapping(source = "printRequest.eroWelsh.name", target = "eroNameCy")
    @Mapping(source = "printRequest.eroWelsh.phoneNumber", target = "eroPhoneNumberCy")
    @Mapping(source = "printRequest.eroWelsh.website", target = "eroWebsiteCy")
    @Mapping(source = "printRequest.eroWelsh.address.property", target = "eroDeliveryPropertyCy")
    @Mapping(source = "printRequest.eroWelsh.address.street", target = "eroDeliveryStreetCy")
    @Mapping(source = "printRequest.eroWelsh.address.town", target = "eroDeliveryTownCy")
    @Mapping(source = "printRequest.eroWelsh.address.locality", target = "eroDeliveryLocalityCy")
    @Mapping(source = "printRequest.eroWelsh.address.area", target = "eroDeliveryAreaCy")
    @Mapping(source = "printRequest.eroWelsh.address.postcode", target = "eroDeliveryPostcodeCy")
    @Mapping(source = "printRequest.eroWelsh.emailAddress", target = "eroEmailAddressCy")
    @Mapping(source = "certificate.issuingAuthorityCy", target = "issuingAuthorityCy")
    @Mapping(source = "printRequest.requestDateTime", target = "requestDateTime")
    @Mapping(source = "photoZipPath", target = "photo")
    abstract fun map(certificate: Certificate, printRequest: uk.gov.dluhc.printapi.database.entity.PrintRequest, photoZipPath: String): PrintRequest
}
