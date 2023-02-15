package uk.gov.dluhc.printapi.service.pdf

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.English
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.PhotoProperties
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.Welsh
import uk.gov.dluhc.printapi.config.TemporaryCertificatePdfTemplateProperties
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_ENGLAND
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_NORTHERN_IRELAND
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_SCOTLAND
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_WALES
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssuingAuthority
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@ExtendWith(MockitoExtension::class)
internal class ElectorDocumentPdfTemplateDetailsFactoryTest {

    companion object {
        // Template paths
        private const val ENGLISH_TEMPLATE_FILENAME = "english-template.pdf"
        private const val WELSH_TEMPLATE_FILENAME = "welsh-template.pdf"
        private const val ENGLISH_TEMPLATE_PATH = "/path/to/$ENGLISH_TEMPLATE_FILENAME"
        private const val WELSH_TEMPLATE_PATH = "/path/to/$WELSH_TEMPLATE_FILENAME"

        // English placeholders
        private const val ENGLISH_PLACEHOLDER_ELECTOR_NAME = "electorNameEnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_LA_NAME = "localAuthorityNameEnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ISSUE_DATE = "dateOfIssueEnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_VALID_ON_DATE = "validOnDateEnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_CERTIFICATE_NUMBER = "certificateNumberEnPlaceholder"
        private const val ENGLISH_IMAGES_VOTER_PHOTO_PAGE_NUMBER = 1
        private const val ENGLISH_IMAGES_VOTER_PHOTO_ABSOLUTE_X = 62f
        private const val ENGLISH_IMAGES_VOTER_PHOTO_ABSOLUTE_Y = 568f
        private const val ENGLISH_IMAGES_VOTER_PHOTO_FIT_WIDTH = 99f
        private const val ENGLISH_IMAGES_VOTER_PHOTO_FIT_HEIGHT = 127f

        // Welsh placeholders
        private const val WELSH_PLACEHOLDER_ELECTOR_NAME = "electorNameCyPlaceholder"
        private const val WELSH_PLACEHOLDER_LA_NAME_EN = "localAuthorityNameEnPlaceholder"
        private const val WELSH_PLACEHOLDER_LA_NAME_CY = "localAuthorityNameCyPlaceholder"
        private const val WELSH_PLACEHOLDER_ISSUE_DATE = "dateOfIssueCyPlaceholder"
        private const val WELSH_PLACEHOLDER_VALID_ON_DATE = "validOnDateCyPlaceholder"
        private const val WELSH_PLACEHOLDER_CERTIFICATE_NUMBER = "certificateNumberCyPlaceholder"
        private const val WELSH_IMAGES_VOTER_PHOTO_PAGE_NUMBER = 2
        private const val WELSH_IMAGES_VOTER_PHOTO_ABSOLUTE_X = 12f
        private const val WELSH_IMAGES_VOTER_PHOTO_ABSOLUTE_Y = 56f
        private const val WELSH_IMAGES_VOTER_PHOTO_FIT_WIDTH = 45f
        private const val WELSH_IMAGES_VOTER_PHOTO_FIT_HEIGHT = 132f

        // LocalDate formatter
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    @Mock
    private lateinit var s3Client: S3Client

    private lateinit var templateSelector: ElectorDocumentPdfTemplateDetailsFactory

    @BeforeEach
    fun setup() {
        templateSelector = ElectorDocumentPdfTemplateDetailsFactory(
            s3Client,
            TemporaryCertificatePdfTemplateProperties(
                english = English(
                    path = ENGLISH_TEMPLATE_PATH,
                    placeholder = English.Placeholder(
                        electorName = ENGLISH_PLACEHOLDER_ELECTOR_NAME,
                        localAuthorityNameEn = ENGLISH_PLACEHOLDER_LA_NAME,
                        dateOfIssue = ENGLISH_PLACEHOLDER_ISSUE_DATE,
                        validOnDate = ENGLISH_PLACEHOLDER_VALID_ON_DATE,
                        certificateNumber = ENGLISH_PLACEHOLDER_CERTIFICATE_NUMBER
                    ),
                    images = English.Images(
                        voterPhoto = PhotoProperties(
                            pageNumber = ENGLISH_IMAGES_VOTER_PHOTO_PAGE_NUMBER,
                            absoluteXMm = ENGLISH_IMAGES_VOTER_PHOTO_ABSOLUTE_X,
                            absoluteYMm = ENGLISH_IMAGES_VOTER_PHOTO_ABSOLUTE_Y,
                            fitWidthMm = ENGLISH_IMAGES_VOTER_PHOTO_FIT_WIDTH,
                            fitHeightMm = ENGLISH_IMAGES_VOTER_PHOTO_FIT_HEIGHT
                        )
                    )
                ),
                welsh = Welsh(
                    path = WELSH_TEMPLATE_PATH,
                    placeholder = Welsh.Placeholder(
                        electorName = WELSH_PLACEHOLDER_ELECTOR_NAME,
                        localAuthorityNameEn = WELSH_PLACEHOLDER_LA_NAME_EN,
                        localAuthorityNameCy = WELSH_PLACEHOLDER_LA_NAME_CY,
                        dateOfIssue = WELSH_PLACEHOLDER_ISSUE_DATE,
                        validOnDate = WELSH_PLACEHOLDER_VALID_ON_DATE,
                        certificateNumber = WELSH_PLACEHOLDER_CERTIFICATE_NUMBER
                    ),
                    images = Welsh.Images(
                        voterPhoto = PhotoProperties(
                            pageNumber = WELSH_IMAGES_VOTER_PHOTO_PAGE_NUMBER,
                            absoluteXMm = WELSH_IMAGES_VOTER_PHOTO_ABSOLUTE_X,
                            absoluteYMm = WELSH_IMAGES_VOTER_PHOTO_ABSOLUTE_Y,
                            fitWidthMm = WELSH_IMAGES_VOTER_PHOTO_FIT_WIDTH,
                            fitHeightMm = WELSH_IMAGES_VOTER_PHOTO_FIT_HEIGHT
                        )
                    )
                )
            )
        )
    }

    @Nested
    inner class GetTemplateFilename {
        @ParameterizedTest
        @CsvSource(value = [GSS_CODE_ENGLAND, GSS_CODE_SCOTLAND, GSS_CODE_NORTHERN_IRELAND])
        fun `should get Template Filename when English template selected`(gssCode: String) {
            // Given

            // When
            val actual = templateSelector.getTemplateFilename(gssCode)

            // Then
            assertThat(actual).isEqualTo(ENGLISH_TEMPLATE_FILENAME)
        }

        @Test
        fun `should get template details when Welsh template selected`() {
            // Given
            val gssCode = GSS_CODE_WALES

            // When
            val actual = templateSelector.getTemplateFilename(gssCode)

            // Then
            assertThat(actual).isEqualTo(WELSH_TEMPLATE_FILENAME)
        }
    }

    @Nested
    inner class GetTemplateDetails {
        @ParameterizedTest
        @CsvSource(value = [GSS_CODE_ENGLAND, GSS_CODE_SCOTLAND, GSS_CODE_NORTHERN_IRELAND])
        fun `should get template details when English template selected`(gssCode: String) {
            // Given
            val issueDate = "20/04/2023"
            val validOnDate = "04/05/2023"
            val photoS3Bucket = "some-s3-bucket"
            val photoS3Path = "path/to/a/photo.png"
            val temporaryCertificate = buildTemporaryCertificate(
                gssCode = gssCode,
                issueDate = LocalDate.parse(issueDate, DATE_TIME_FORMATTER),
                validOnDate = LocalDate.parse(validOnDate, DATE_TIME_FORMATTER),
                photoLocationArn = aPhotoArn(bucket = photoS3Bucket, path = photoS3Path)
            )
            val expectedPlaceholders = with(temporaryCertificate) {
                mapOf(
                    ENGLISH_PLACEHOLDER_ELECTOR_NAME to this.getNameOnCertificate(),
                    ENGLISH_PLACEHOLDER_LA_NAME to issuingAuthority,
                    ENGLISH_PLACEHOLDER_ISSUE_DATE to issueDate,
                    ENGLISH_PLACEHOLDER_VALID_ON_DATE to validOnDate,
                    ENGLISH_PLACEHOLDER_CERTIFICATE_NUMBER to certificateNumber,
                )
            }
            val photoBytes = byteArrayOf()
            mockS3PhotoResponse(photoBytes)
            val expectedImages = listOf(
                ImageDetails(
                    ENGLISH_IMAGES_VOTER_PHOTO_PAGE_NUMBER,
                    ENGLISH_IMAGES_VOTER_PHOTO_ABSOLUTE_X,
                    ENGLISH_IMAGES_VOTER_PHOTO_ABSOLUTE_Y,
                    ENGLISH_IMAGES_VOTER_PHOTO_FIT_WIDTH,
                    ENGLISH_IMAGES_VOTER_PHOTO_FIT_HEIGHT,
                    photoBytes
                )
            )

            // When
            val actual = templateSelector.getTemplateDetails(temporaryCertificate)

            // Then
            verify(s3Client).getObjectAsBytes(GetObjectRequest.builder().bucket(photoS3Bucket).key(photoS3Path).build())
            assertThat(actual.path).isEqualTo(ENGLISH_TEMPLATE_PATH)
            assertThat(actual.placeholders).isEqualTo(expectedPlaceholders)
            assertThat(actual.images).isEqualTo(expectedImages)
        }

        @Test
        fun `should get template details when Welsh template selected`() {
            // Given
            val issueDate = "20/04/2023"
            val validOnDate = "04/05/2023"
            val photoS3Bucket = "some-s3-bucket"
            val photoS3Path = "path/to/a/photo.png"
            val temporaryCertificate = buildTemporaryCertificate(
                gssCode = GSS_CODE_WALES,
                issuingAuthority = aValidIssuingAuthority(),
                issuingAuthorityCy = aValidIssuingAuthority(),
                issueDate = LocalDate.parse(issueDate, DATE_TIME_FORMATTER),
                validOnDate = LocalDate.parse(validOnDate, DATE_TIME_FORMATTER),
                photoLocationArn = aPhotoArn(bucket = photoS3Bucket, path = photoS3Path)
            )
            val expectedPlaceholders = with(temporaryCertificate) {
                mapOf(
                    WELSH_PLACEHOLDER_ELECTOR_NAME to this.getNameOnCertificate(),
                    WELSH_PLACEHOLDER_LA_NAME_EN to issuingAuthority,
                    WELSH_PLACEHOLDER_LA_NAME_CY to issuingAuthorityCy,
                    WELSH_PLACEHOLDER_ISSUE_DATE to issueDate,
                    WELSH_PLACEHOLDER_VALID_ON_DATE to validOnDate,
                    WELSH_PLACEHOLDER_CERTIFICATE_NUMBER to certificateNumber,
                )
            }
            val photoBytes = byteArrayOf()
            mockS3PhotoResponse(photoBytes)
            val expectedImages = listOf(
                ImageDetails(
                    WELSH_IMAGES_VOTER_PHOTO_PAGE_NUMBER,
                    WELSH_IMAGES_VOTER_PHOTO_ABSOLUTE_X,
                    WELSH_IMAGES_VOTER_PHOTO_ABSOLUTE_Y,
                    WELSH_IMAGES_VOTER_PHOTO_FIT_WIDTH,
                    WELSH_IMAGES_VOTER_PHOTO_FIT_HEIGHT,
                    photoBytes
                )
            )

            // When
            val actual = templateSelector.getTemplateDetails(temporaryCertificate)

            // Then
            verify(s3Client).getObjectAsBytes(GetObjectRequest.builder().bucket(photoS3Bucket).key(photoS3Path).build())
            assertThat(actual.path).isEqualTo(WELSH_TEMPLATE_PATH)
            assertThat(actual.placeholders).isEqualTo(expectedPlaceholders)
            assertThat(actual.images).isEqualTo(expectedImages)
        }
    }

    private fun mockS3PhotoResponse(photoBytes: ByteArray) {
        val response = ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), photoBytes)
        given(s3Client.getObjectAsBytes(any<GetObjectRequest>())).willReturn(response)
    }
}
