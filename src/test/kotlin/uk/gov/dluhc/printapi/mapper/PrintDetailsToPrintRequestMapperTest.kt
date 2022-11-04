package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.dluhc.printapi.database.entity.CertificateFormat
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest.DeliveryOption
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificateDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoZipPath
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.Date
import java.util.UUID
import java.util.stream.Stream

class PrintDetailsToPrintRequestMapperTest {
    private val mapper = PrintDetailsToPrintRequestMapperImpl()

    companion object {
        @JvmStatic
        private fun welshEro(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(null),
                Arguments.of(buildElectoralRegistrationOffice(name = aValidLocalAuthorityName())),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("welshEro")
    fun `should map ERO response to dto with optional Welsh ERO translation`(eroWelsh: ElectoralRegistrationOffice?) {
        // Given
        val id = UUID.fromString("7787ba5b-e79e-40c2-bcc4-6118393628bc")
        val requestId: String = aValidRequestId()
        val sourceReference: String = aValidSourceReference()
        val applicationReference: String = aValidApplicationReference()
        val vacNumber: String = aValidVacNumber()
        val vacVersion = "1"
        val sourceType: SourceType = SourceType.VOTER_CARD
        val requestDateTime: OffsetDateTime = Instant.ofEpochMilli(0).atOffset(UTC)
        val firstName = "John"
        val middleNames = "Anthony Barry"
        val surname = "Doe"
        val certificateLanguage = CertificateLanguage.EN
        val certificateFormat = CertificateFormat.STANDARD
        val delivery = buildCertificateDelivery()
        val gssCode: String = getRandomGssCode()
        val issuingAuthority: String = aValidLocalAuthorityName()
        val issueDate = LocalDate.of(2022, 10, 21)
        val eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(name = issuingAuthority)
        val photoLocation = aPhotoArn()
        val status = Status.PENDING_ASSIGNMENT_TO_BATCH
        val batchId = aValidBatchId()
        val details = PrintDetails(
            id = id,
            requestId = requestId,
            sourceReference = sourceReference,
            applicationReference = applicationReference,
            sourceType = sourceType,
            vacNumber = vacNumber,
            vacVersion = vacVersion,
            requestDateTime = requestDateTime,
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            certificateLanguage = certificateLanguage,
            certificateFormat = certificateFormat,
            photoLocation = photoLocation,
            delivery = delivery,
            gssCode = gssCode,
            issuingAuthority = issuingAuthority,
            issueDate = issueDate,
            eroEnglish = eroEnglish,
            eroWelsh = eroWelsh,
            batchId = batchId,
            status = status,
        )
        val photoZipPath: String = aPhotoZipPath()

        val expected = PrintRequest()
        expected.requestId = requestId
        expected.issuingAuthorityEn = eroEnglish.name
        expected.issueDate = LocalDate.parse("2022-10-21")
        expected.suggestedExpiryDate = LocalDate.parse("2032-10-21")
        expected.requestDateTime = requestDateTime
        expected.cardFirstname = firstName
        expected.cardMiddleNames = middleNames
        expected.cardSurname = surname
        expected.cardVersion = vacVersion
        expected.cardNumber = vacNumber
        expected.certificateLanguage = PrintRequest.CertificateLanguage.EN
        expected.certificateFormat = PrintRequest.CertificateFormat.STANDARD
        expected.deliveryOption = DeliveryOption.STANDARD
        expected.photo = photoZipPath
        expected.deliveryName = delivery.addressee
        expected.deliveryStreet = delivery.address?.street
        expected.deliveryProperty = delivery.address?.property
        expected.deliveryLocality = delivery.address?.locality
        expected.deliveryTown = delivery.address?.town
        expected.deliveryArea = delivery.address?.area
        expected.deliveryPostcode = delivery.address?.postcode
        expected.eroNameEn = eroEnglish.name
        expected.eroPhoneNumberEn = eroEnglish.phoneNumber
        expected.eroEmailAddressEn = eroEnglish.emailAddress
        expected.eroWebsiteEn = eroEnglish.website
        expected.eroDeliveryStreetEn = eroEnglish.address?.street
        expected.eroDeliveryPropertyEn = eroEnglish.address?.property
        expected.eroDeliveryLocalityEn = eroEnglish.address?.locality
        expected.eroDeliveryTownEn = eroEnglish.address?.town
        expected.eroDeliveryAreaEn = eroEnglish.address?.area
        expected.eroDeliveryPostcodeEn = eroEnglish.address?.postcode
        // Optional Welsh translation expectations
        expected.issuingAuthorityCy = eroWelsh?.name
        expected.eroNameCy = eroWelsh?.name
        expected.eroPhoneNumberCy = eroWelsh?.phoneNumber
        expected.eroEmailAddressCy = eroWelsh?.emailAddress
        expected.eroWebsiteCy = eroWelsh?.website
        expected.eroDeliveryStreetCy = eroWelsh?.address?.street
        expected.eroDeliveryPropertyCy = eroWelsh?.address?.property
        expected.eroDeliveryLocalityCy = eroWelsh?.address?.locality
        expected.eroDeliveryTownCy = eroWelsh?.address?.town
        expected.eroDeliveryAreaCy = eroWelsh?.address?.area
        expected.eroDeliveryPostcodeCy = eroWelsh?.address?.postcode

        // When
        val actual = mapper.map(details, photoZipPath)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }

    @Test
    fun `should map OffsetDateTime to Date`() {
        // Given
        val instant = Instant.now()
        val offset = instant.atOffset(UTC)
        val expectedDate = Date.from(instant)

        // When
        val actual = mapper.map(offset)

        // Then
        assertThat(actual).isEqualTo(expectedDate)
    }
}
