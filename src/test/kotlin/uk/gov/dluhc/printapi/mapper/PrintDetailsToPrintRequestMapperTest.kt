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
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificateDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.getAMongoDbId
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoLocation
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
        val requestId: String = getAMongoDbId()
        val sourceReference: String = getAMongoDbId()
        val applicationReference: String = aValidApplicationReference()
        val vacNumber: String = getAMongoDbId()
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
        val photoLocation = "arn:aws:s3:::dev-vca-api-vca-target-bucket/E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"
        val status = Status.PENDING_ASSIGNMENT_TO_BATCH
        val batchId = "8b2215c7-4e1e-4f0e-873b-a2eea04eac84"
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
        val photoZipPath: String = aPhotoLocation().zipPath

        val expected = PrintRequest()
        expected.requestId = requestId
        expected.issuingAuthorityEn = eroEnglish.name
        expected.issueDate = "2022-10-21"
        expected.suggestedExpiryDate = "2032-10-21"
        expected.requestDateTime = Date.from(requestDateTime.toInstant())
        expected.cardFirstname = firstName
        expected.cardMiddlenames = middleNames
        expected.cardSurname = surname
        expected.cardVersion = vacVersion
        expected.cardNumber = vacNumber
        expected.certificateLanguage = PrintRequest.CertificateLanguage.EN
        expected.certificateFormat = PrintRequest.CertificateFormat.STANDARD
        expected.deliveryOption = DeliveryOption.STANDARD
        expected.photo = photoZipPath
        expected.deliveryName = delivery.addressee
        expected.deliveryStreet = delivery.address?.street
        expected.deliverypProperty = delivery.address?.property
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
        expected.eroDeliverypPropertyCy = eroWelsh?.address?.property
        expected.eroDeliveryLocalityCy = eroWelsh?.address?.locality
        expected.eroDeliveryTownCy = eroWelsh?.address?.town
        expected.erodDeliveryAreaCy = eroWelsh?.address?.area
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
