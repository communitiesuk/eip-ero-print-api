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
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedPrintRequest
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntity

@ExtendWith(MockitoExtension::class)
class AnonymousElectorDocumentMapperTest {

    @InjectMocks
    private lateinit var mapper: AnonymousElectorDocumentMapperImpl

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: SupportingInformationFormatMapper

    @Mock
    private lateinit var printRequestMapper: AedPrintRequestMapper

    @Mock
    private lateinit var idFactory: IdFactory

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

        val printRequest = buildAedPrintRequest()
        given(printRequestMapper.toPrintRequest(any(), any())).willReturn(printRequest)

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
                printRequests = mutableListOf(printRequest),
            )
        }

        // When
        val actual = mapper.toAnonymousElectorDocument(request, aedTemplateFilename)

        // Then
        verify(sourceTypeMapper).mapDtoToEntity(request.sourceType)
        verify(certificateLanguageMapper).mapDtoToEntity(request.certificateLanguage)
        verify(supportingInformationFormatMapper).mapDtoToEntity(request.supportingInformationFormat)
        verify(idFactory).vacNumber()
        verify(printRequestMapper).toPrintRequest(request, aedTemplateFilename)
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
