package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.getAMongoDbId
import java.time.Instant
import java.util.Date

fun buildPrintRequest(
    eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(),
    eroWelsh: ElectoralRegistrationOffice? = buildElectoralRegistrationOffice(),
    requestId: String = getAMongoDbId(),
    issuingAuthorityEn: String = eroEnglish.name!!,
    issuingAuthorityCy: String = eroWelsh?.name!!,
    issueDate: String = "12/07/2023",
    suggestedExpiryDate: String = "28/02/2033",
    requestDateTime: Date = Date.from(Instant.now()),
    cardFirstname: String = faker.name().firstName(),
    cardMiddlenames: String? = faker.name().firstName(),
    cardSurname: String = faker.name().lastName(),
    cardVersion: String = "1",
    cardNumber: String = "3987",
    certificateLanguage: PrintRequest.CertificateLanguage = PrintRequest.CertificateLanguage.EN,
    certificateFormat: PrintRequest.CertificateFormat = PrintRequest.CertificateFormat.STANDARD,
    deliveryOption: PrintRequest.DeliveryOption = PrintRequest.DeliveryOption.STANDARD,
    photo: String = "8a53a30ac9bae2ebb9b1239b.png",
    deliveryName: String = faker.name().name(),
    deliveryProperty: String? = faker.address().buildingNumber(),
    deliveryLocality: String? = faker.address().streetName(),
    deliveryTown: String? = faker.address().city(),
    deliveryArea: String? = faker.address().state(),
    deliveryStreet: String = faker.address().streetName(),
    deliveryPostcode: String = faker.address().postcode(),
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
    eroDeliverypPropertyCy: String? = eroWelsh?.address?.property,
    eroDeliveryLocalityCy: String? = eroWelsh?.address?.locality,
    eroDeliveryTownCy: String? = eroWelsh?.address?.town,
    erodDeliveryAreaCy: String? = eroWelsh?.address?.area,
    eroDeliveryPostcodeCy: String? = eroWelsh?.address?.postcode,
): PrintRequest {
    val request = PrintRequest()
    request.requestId = requestId
    request.issuingAuthorityEn = issuingAuthorityEn
    request.issueDate = issueDate
    request.suggestedExpiryDate = suggestedExpiryDate
    request.requestDateTime = Date.from(requestDateTime.toInstant())
    request.cardFirstname = cardFirstname
    request.cardMiddlenames = cardMiddlenames
    request.cardSurname = cardSurname
    request.cardVersion = cardVersion
    request.cardNumber = cardNumber
    request.certificateLanguage = certificateLanguage
    request.certificateFormat = certificateFormat
    request.deliveryOption = deliveryOption
    request.photo = photo
    request.deliveryName = deliveryName
    request.deliveryStreet = deliveryStreet
    request.deliverypProperty = deliveryProperty
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
    request.eroDeliverypPropertyCy = eroDeliverypPropertyCy
    request.eroDeliveryLocalityCy = eroDeliveryLocalityCy
    request.eroDeliveryTownCy = eroDeliveryTownCy
    request.erodDeliveryAreaCy = erodDeliveryAreaCy
    request.eroDeliveryPostcodeCy = eroDeliveryPostcodeCy
    return request
}

fun aPrintRequest(): PrintRequest = buildPrintRequest()

fun aPrintRequestList(): List<PrintRequest> = listOf(buildPrintRequest())
