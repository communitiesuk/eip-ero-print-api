package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateAnonymousElectorDocumentDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntity

@ExtendWith(MockitoExtension::class)
class AnonymousElectorDocumentMapperTest {

    companion object {
        private const val FIXED_DATE_STRING = "2022-10-18"
        private val FIXED_DATE = LocalDate.parse(FIXED_DATE_STRING)
        private val FIXED_TIME = Instant.parse("${FIXED_DATE_STRING}T11:22:32.123Z")
        private val FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
    }

    @InjectMocks
    private lateinit var mapper: AnonymousElectorDocumentMapperImpl

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: SupportingInformationFormatMapper

    @Mock
    private lateinit var idFactory: IdFactory

    @Spy
    private val clock: Clock = FIXED_CLOCK

    @Test
    fun `should map send application to print message for an English certificate to print details`() {
        // Given
        val request = buildGenerateAnonymousElectorDocumentDto()
        val aedTemplateFilename = aValidAnonymousElectorDocumentTemplateFilename()
        val certificateNumber = aValidVacNumber()
        given(sourceTypeMapper.mapDtoToEntity(any())).willReturn(SourceTypeEntity.VOTER_CARD)
        given(certificateLanguageMapper.mapDtoToEntity(any())).willReturn(CertificateLanguage.EN)
        given(supportingInformationFormatMapper.mapDtoToEntity(any())).willReturn(SupportingInformationFormat.STANDARD)
        given(idFactory.vacNumber()).willReturn(certificateNumber)

        val expected = with(request) {
            AnonymousElectorDocument(
                id = null,
                gssCode = gssCode,
                sourceType = SourceTypeEntity.VOTER_CARD,
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                certificateLanguage = CertificateLanguageEntity.EN,
                supportingInformationFormat = SupportingInformationFormatEntity.STANDARD,
                certificateNumber = certificateNumber,
                photoLocationArn = photoLocation,
                electoralRollNumber = electoralRollNumber,
                issueDate = FIXED_DATE,
                aedTemplateFilename = aedTemplateFilename,
                requestDateTime = FIXED_TIME,
                contactDetails = AedContactDetails(
                    firstName = firstName,
                    middleNames = middleNames,
                    surname = surname,
                    email = email,
                    phoneNumber = phoneNumber,
                    address = with(address) {
                        Address(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    },
                ),
                userId = userId,
                statusHistory = mutableListOf(
                    AnonymousElectorDocumentStatus(
                        status = AnonymousElectorDocumentStatus.Status.PRINTED,
                        eventDateTime = FIXED_TIME
                    )
                )
            )
        }

        // When
        val actual = mapper.toAnonymousElectorDocument(request, aedTemplateFilename)

        // Then
        verify(sourceTypeMapper).mapDtoToEntity(request.sourceType)
        verify(certificateLanguageMapper).mapDtoToEntity(request.certificateLanguage)
        verify(supportingInformationFormatMapper).mapDtoToEntity(request.supportingInformationFormat!!)
        verify(idFactory).vacNumber()
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
