package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import net.datafaker.providers.base.Address as FakerAddress
import net.datafaker.providers.base.Name as FakerName

fun buildPrintRequest(
    eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(),
    eroWelsh: ElectoralRegistrationOffice? = buildElectoralRegistrationOffice(),
    requestId: String = aValidRequestId(),
    issuingAuthorityEn: String = eroEnglish.name!!,
    issuingAuthorityCy: String? = eroWelsh?.name!!,
    issueDate: LocalDate = LocalDate.parse("2023-07-12"),
    suggestedExpiryDate: LocalDate = LocalDate.parse("2033-02-28"),
    requestDateTime: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    fakeName: FakerName = faker.name(),
    cardFirstname: String = fakeName.firstName(),
    cardMiddleNames: String? = fakeName.firstName(),
    cardSurname: String = fakeName.lastName(),
    cardVersion: String = "1",
    cardNumber: String = "3987",
    certificateLanguage: PrintRequest.CertificateLanguage = PrintRequest.CertificateLanguage.EN,
    certificateFormat: PrintRequest.CertificateFormat = PrintRequest.CertificateFormat.STANDARD,
    deliveryOption: PrintRequest.DeliveryOption = PrintRequest.DeliveryOption.STANDARD,
    photo: String = "8a53a30ac9bae2ebb9b1239b.png",
    deliveryName: String = fakeName.name(),
    fakeAddress: FakerAddress = faker.address(),
    deliveryProperty: String? = fakeAddress.buildingNumber(),
    deliveryLocality: String? = fakeAddress.streetName(),
    deliveryTown: String? = fakeAddress.city(),
    deliveryArea: String? = fakeAddress.state(),
    deliveryStreet: String = fakeAddress.streetName(),
    deliveryPostcode: String = fakeAddress.postcode(),
    eroNameEn: String = eroEnglish.name!!,
    eroPhoneNumberEn: String = eroEnglish.phoneNumber!!,
    eroEmailAddressEn: String = eroEnglish.emailAddress!!,
    eroWebsiteEn: String = eroEnglish.website!!,
    eroDeliveryStreetEn: String = eroEnglish.address!!.street!!,
    eroDeliveryPropertyEn: String? = eroEnglish.address!!.property!!,
    eroDeliveryLocalityEn: String? = eroEnglish.address!!.locality!!,
    eroDeliveryTownEn: String? = eroEnglish.address!!.town!!,
    eroDeliveryAreaEn: String? = eroEnglish.address!!.area!!,
    eroDeliveryPostcodeEn: String = eroEnglish.address!!.postcode!!,
    eroNameCy: String? = eroWelsh?.name,
    eroPhoneNumberCy: String? = eroWelsh?.phoneNumber,
    eroEmailAddressCy: String? = eroWelsh?.emailAddress,
    eroWebsiteCy: String? = eroWelsh?.website,
    eroDeliveryStreetCy: String? = eroWelsh?.address?.street,
    eroDeliveryPropertyCy: String? = eroWelsh?.address?.property,
    eroDeliveryLocalityCy: String? = eroWelsh?.address?.locality,
    eroDeliveryTownCy: String? = eroWelsh?.address?.town,
    eroDeliveryAreaCy: String? = eroWelsh?.address?.area,
    eroDeliveryPostcodeCy: String? = eroWelsh?.address?.postcode,
): PrintRequest {
    val request = PrintRequest()
    request.requestId = requestId
    request.issuingAuthorityEn = issuingAuthorityEn
    request.issueDate = issueDate
    request.suggestedExpiryDate = suggestedExpiryDate
    request.requestDateTime = requestDateTime
    request.cardFirstname = cardFirstname
    request.cardMiddleNames = cardMiddleNames
    request.cardSurname = cardSurname
    request.cardVersion = cardVersion
    request.cardNumber = cardNumber
    request.certificateLanguage = certificateLanguage
    request.certificateFormat = certificateFormat
    request.deliveryOption = deliveryOption
    request.photo = photo
    request.deliveryName = deliveryName
    request.deliveryStreet = deliveryStreet
    request.deliveryProperty = deliveryProperty
    request.deliveryLocality = deliveryLocality
    request.deliveryTown = deliveryTown
    request.deliveryArea = deliveryArea
    request.deliveryPostcode = deliveryPostcode
    request.eroNameEn = eroNameEn
    request.eroPhoneNumberEn = eroPhoneNumberEn
    request.eroEmailAddressEn = eroEmailAddressEn
    request.eroWebsiteEn = eroWebsiteEn
    request.eroDeliveryStreetEn = eroDeliveryStreetEn
    request.eroDeliveryPropertyEn = eroDeliveryPropertyEn
    request.eroDeliveryLocalityEn = eroDeliveryLocalityEn
    request.eroDeliveryTownEn = eroDeliveryTownEn
    request.eroDeliveryAreaEn = eroDeliveryAreaEn
    request.eroDeliveryPostcodeEn = eroDeliveryPostcodeEn
    request.issuingAuthorityCy = issuingAuthorityCy
    request.eroNameCy = eroNameCy
    request.eroPhoneNumberCy = eroPhoneNumberCy
    request.eroEmailAddressCy = eroEmailAddressCy
    request.eroWebsiteCy = eroWebsiteCy
    request.eroDeliveryStreetCy = eroDeliveryStreetCy
    request.eroDeliveryPropertyCy = eroDeliveryPropertyCy
    request.eroDeliveryLocalityCy = eroDeliveryLocalityCy
    request.eroDeliveryTownCy = eroDeliveryTownCy
    request.eroDeliveryAreaCy = eroDeliveryAreaCy
    request.eroDeliveryPostcodeCy = eroDeliveryPostcodeCy
    return request
}

fun aPrintRequest(): PrintRequest = buildPrintRequest()

fun aPrintRequestList(): List<PrintRequest> = listOf(buildPrintRequest())
