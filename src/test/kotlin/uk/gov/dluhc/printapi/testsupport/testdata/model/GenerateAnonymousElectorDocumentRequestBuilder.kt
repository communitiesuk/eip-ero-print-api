package uk.gov.dluhc.printapi.testsupport.testdata.model

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.printapi.models.Address
import uk.gov.dluhc.printapi.models.CertificateDelivery
import uk.gov.dluhc.printapi.models.CertificateLanguage
import uk.gov.dluhc.printapi.models.GenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.models.SourceType
import uk.gov.dluhc.printapi.models.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import net.datafaker.providers.base.Address as DataFakerAddress

fun buildGenerateAnonymousElectorDocumentRequest(
    gssCode: String = aGssCode(),
    sourceType: SourceType = SourceType.ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT,
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    photoLocation: String = aPhotoArn(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    supportingInformationFormat: SupportingInformationFormat = SupportingInformationFormat.STANDARD,
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    email: String? = aValidEmailAddress(),
    phoneNumber: String? = aValidPhoneNumber(),
    registeredAddress: Address = buildValidAddress(),
    delivery: CertificateDelivery = buildApiCertificateDelivery()
): GenerateAnonymousElectorDocumentRequest =
    GenerateAnonymousElectorDocumentRequest(
        gssCode = gssCode,
        sourceType = sourceType,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        electoralRollNumber = electoralRollNumber,
        photoLocation = photoLocation,
        certificateLanguage = certificateLanguage,
        supportingInformationFormat = supportingInformationFormat,
        firstName = firstName,
        middleNames = middleNames,
        surname = surname,
        email = email,
        phoneNumber = phoneNumber,
        registeredAddress = registeredAddress,
        delivery = delivery
    )

fun buildValidAddress(
    fakeAddress: DataFakerAddress = DataFaker.faker.address(),
    property: String? = fakeAddress.buildingNumber(),
    street: String = fakeAddress.streetName(),
    locality: String? = fakeAddress.streetName(),
    town: String? = fakeAddress.city(),
    area: String? = fakeAddress.state(),
    postcode: String = fakeAddress.postcode(),
    uprn: String? = RandomStringUtils.randomNumeric(12),
) = Address(
    property = property,
    street = street,
    town = town,
    area = area,
    locality = locality,
    uprn = uprn,
    postcode = postcode
)
