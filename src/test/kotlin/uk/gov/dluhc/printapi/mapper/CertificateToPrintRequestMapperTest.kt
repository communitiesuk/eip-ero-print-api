package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoZipPath
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateFormat as PrintRequestCertificateFormat
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateLanguage as PrintRequestCertificateLanguage

@ExtendWith(MockitoExtension::class)
class CertificateToPrintRequestMapperTest {

    @InjectMocks
    private lateinit var mapper: CertificateToPrintRequestMapperImpl

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: SupportingInformationFormatMapper

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    companion object {
        private val requestId: String = aValidRequestId()
        private val sourceReference: String = aValidSourceReference()
        private val applicationReference: String = aValidApplicationReference()
        private val vacNumber: String = aValidVacNumber()
        private val vacVersion = "1"
        private val sourceType: SourceType = SourceType.VOTER_CARD
        private val requestDateTime: Instant = Instant.ofEpochMilli(0)
        private val firstName = "John"
        private val middleNames = "Anthony Barry"
        private val surname = "Doe"
        private val supportingInformationFormat = SupportingInformationFormat.STANDARD
        private val delivery = buildDelivery()
        private val gssCode: String = getRandomGssCode()
        private val issuingAuthorityEn = aValidLocalAuthorityName()
        private val issueDate = LocalDate.of(2022, 10, 21)
        private val suggestedExpiryDate = LocalDate.of(2032, 10, 21)
        private val photoLocation = aPhotoArn()
        private val statusHistory = mutableListOf(
            PrintRequestStatus(
                status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                eventDateTime = Instant.now()
            )
        )
        private val batchId = aValidBatchId()
        private val photoZipPath: String = aPhotoZipPath()
    }

    @Test
    fun `should map to print request with English ERO`() {
        // Given
        given(certificateLanguageMapper.mapEntityToPrintRequest(any())).willReturn(PrintRequestCertificateLanguage.EN)

        given(supportingInformationFormatMapper.toPrintRequestApiEnum(any())).willReturn(PrintRequestCertificateFormat.STANDARD)
        given(instantMapper.toOffsetDateTime(any())).willReturn(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0), UTC))

        val certificateLanguage = CertificateLanguage.EN

        val eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(name = issuingAuthorityEn)

        val printRequest = PrintRequest(
            requestId = requestId,
            vacVersion = vacVersion,
            requestDateTime = requestDateTime,
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            certificateLanguage = certificateLanguage,
            supportingInformationFormat = supportingInformationFormat,
            photoLocationArn = photoLocation,
            delivery = delivery,
            eroEnglish = eroEnglish,
            eroWelsh = null,
            batchId = batchId,
            userId = aValidUserId(),
            statusHistory = statusHistory
        )
        val certificate = Certificate(
            vacNumber = vacNumber,
            sourceType = sourceType,
            sourceReference = sourceReference,
            applicationReference = applicationReference,
            applicationReceivedDateTime = Instant.now(),
            issuingAuthority = issuingAuthorityEn,
            issuingAuthorityCy = null,
            issueDate = issueDate,
            suggestedExpiryDate = suggestedExpiryDate,
            status = Status.PENDING_ASSIGNMENT_TO_BATCH,
            gssCode = gssCode,
            printRequests = mutableListOf(printRequest)
        )

        val expected = uk.gov.dluhc.printapi.printprovider.models.PrintRequest()
        expected.requestId = requestId
        expected.issuingAuthorityEn = eroEnglish.name
        expected.issueDate = issueDate
        expected.suggestedExpiryDate = suggestedExpiryDate
        expected.requestDateTime = requestDateTime.atOffset(UTC)
        expected.cardFirstname = firstName
        expected.cardMiddleNames = middleNames
        expected.cardSurname = surname
        expected.cardVersion = vacVersion
        expected.cardNumber = vacNumber
        expected.certificateLanguage = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateLanguage.EN
        expected.certificateFormat = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateFormat.STANDARD
        expected.deliveryOption = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.DeliveryOption.STANDARD
        expected.photo = photoZipPath
        expected.deliveryName = delivery.addressee
        expected.deliveryStreet = delivery.address.street
        expected.deliveryProperty = delivery.address.property
        expected.deliveryLocality = delivery.address.locality
        expected.deliveryTown = delivery.address.town
        expected.deliveryArea = delivery.address.area
        expected.deliveryPostcode = delivery.address.postcode
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
        // Welsh fields
        expected.issuingAuthorityCy = null
        expected.eroNameCy = null
        expected.eroPhoneNumberCy = null
        expected.eroEmailAddressCy = null
        expected.eroWebsiteCy = null
        expected.eroDeliveryStreetCy = null
        expected.eroDeliveryPropertyCy = null
        expected.eroDeliveryLocalityCy = null
        expected.eroDeliveryTownCy = null
        expected.eroDeliveryAreaCy = null
        expected.eroDeliveryPostcodeCy = null

        // When
        val actual = mapper.map(certificate, printRequest, photoZipPath)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
        verify(supportingInformationFormatMapper).toPrintRequestApiEnum(supportingInformationFormat)
        verify(certificateLanguageMapper).mapEntityToPrintRequest(certificateLanguage)
    }

    @Test
    fun `should map to print request with Welsh ERO`() {
        // Given
        given(certificateLanguageMapper.mapEntityToPrintRequest(any())).willReturn(PrintRequestCertificateLanguage.CY)

        given(supportingInformationFormatMapper.toPrintRequestApiEnum(any())).willReturn(PrintRequestCertificateFormat.STANDARD)
        given(instantMapper.toOffsetDateTime(any())).willReturn(OffsetDateTime.ofInstant(Instant.ofEpochMilli(0), UTC))

        val certificateLanguage = CertificateLanguage.CY

        val issuingAuthorityCy = aValidLocalAuthorityName()

        val eroWelsh = buildElectoralRegistrationOffice(name = issuingAuthorityCy)
        val eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(name = issuingAuthorityEn)

        val printRequest = PrintRequest(
            requestId = requestId,
            vacVersion = vacVersion,
            requestDateTime = requestDateTime,
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            certificateLanguage = certificateLanguage,
            supportingInformationFormat = supportingInformationFormat,
            photoLocationArn = photoLocation,
            delivery = delivery,
            eroEnglish = eroEnglish,
            eroWelsh = eroWelsh,
            batchId = batchId,
            userId = aValidUserId(),
            statusHistory = statusHistory
        )
        val certificate = Certificate(
            vacNumber = vacNumber,
            sourceType = sourceType,
            sourceReference = sourceReference,
            applicationReference = applicationReference,
            applicationReceivedDateTime = Instant.now(),
            issuingAuthority = issuingAuthorityEn,
            issuingAuthorityCy = issuingAuthorityCy,
            issueDate = issueDate,
            suggestedExpiryDate = suggestedExpiryDate,
            status = Status.PENDING_ASSIGNMENT_TO_BATCH,
            gssCode = gssCode,
            printRequests = mutableListOf(printRequest)
        )

        val expected = uk.gov.dluhc.printapi.printprovider.models.PrintRequest()
        expected.requestId = requestId
        expected.issuingAuthorityEn = eroEnglish.name
        expected.issueDate = issueDate
        expected.suggestedExpiryDate = suggestedExpiryDate
        expected.requestDateTime = requestDateTime.atOffset(UTC)
        expected.cardFirstname = firstName
        expected.cardMiddleNames = middleNames
        expected.cardSurname = surname
        expected.cardVersion = vacVersion
        expected.cardNumber = vacNumber
        expected.certificateLanguage = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateLanguage.CY
        expected.certificateFormat = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.CertificateFormat.STANDARD
        expected.deliveryOption = uk.gov.dluhc.printapi.printprovider.models.PrintRequest.DeliveryOption.STANDARD
        expected.photo = photoZipPath
        expected.deliveryName = delivery.addressee
        expected.deliveryStreet = delivery.address.street
        expected.deliveryProperty = delivery.address.property
        expected.deliveryLocality = delivery.address.locality
        expected.deliveryTown = delivery.address.town
        expected.deliveryArea = delivery.address.area
        expected.deliveryPostcode = delivery.address.postcode
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
        // Welsh fields
        expected.issuingAuthorityCy = eroWelsh.name
        expected.eroNameCy = eroWelsh.name
        expected.eroPhoneNumberCy = eroWelsh.phoneNumber
        expected.eroEmailAddressCy = eroWelsh.emailAddress
        expected.eroWebsiteCy = eroWelsh.website
        expected.eroDeliveryStreetCy = eroWelsh.address?.street
        expected.eroDeliveryPropertyCy = eroWelsh.address?.property
        expected.eroDeliveryLocalityCy = eroWelsh.address?.locality
        expected.eroDeliveryTownCy = eroWelsh.address?.town
        expected.eroDeliveryAreaCy = eroWelsh.address?.area
        expected.eroDeliveryPostcodeCy = eroWelsh.address?.postcode

        // When
        val actual = mapper.map(certificate, printRequest, photoZipPath)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
        verify(supportingInformationFormatMapper).toPrintRequestApiEnum(supportingInformationFormat)
        verify(certificateLanguageMapper).mapEntityToPrintRequest(certificateLanguage)
    }
}
