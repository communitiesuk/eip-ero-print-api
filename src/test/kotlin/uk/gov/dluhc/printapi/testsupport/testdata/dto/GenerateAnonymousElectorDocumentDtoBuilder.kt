package uk.gov.dluhc.printapi.testsupport.testdata.dto

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.CertificateLanguage
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.SourceType
import uk.gov.dluhc.printapi.dto.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguageDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceTypeDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn

fun buildGenerateAnonymousElectorDocumentDto(
    gssCode: String = aGssCode(),
    sourceType: SourceType = aValidSourceTypeDto(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    photoLocation: String = aPhotoArn(),
    certificateLanguage: CertificateLanguage = aValidCertificateLanguageDto(),
    supportingInformationFormat: SupportingInformationFormat = SupportingInformationFormat.STANDARD,
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    email: String = aValidEmailAddress(),
    phoneNumber: String = aValidPhoneNumber(),
    address: AddressDto = buildValidAddressDto(),
    userId: String = aValidUserId(),
): GenerateAnonymousElectorDocumentDto =
    GenerateAnonymousElectorDocumentDto(
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
        address = address,
        userId = userId
    )

fun buildValidAddressDto(
    fakeAddress: net.datafaker.Address = DataFaker.faker.address(),
    property: String? = fakeAddress.buildingNumber(),
    street: String = fakeAddress.streetName(),
    locality: String? = fakeAddress.streetName(),
    town: String? = fakeAddress.city(),
    area: String? = fakeAddress.state(),
    postcode: String = fakeAddress.postcode(),
    uprn: String? = RandomStringUtils.randomNumeric(12),
): AddressDto =
    AddressDto(
        property = property,
        street = street,
        town = town,
        area = area,
        locality = locality,
        uprn = uprn,
        postcode = postcode
    )