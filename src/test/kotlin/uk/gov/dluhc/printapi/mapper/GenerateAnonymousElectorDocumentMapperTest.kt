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
import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.dto.CertificateLanguage as CertificateLanguageDto
import uk.gov.dluhc.printapi.dto.SourceType as SourceTypeDto
import uk.gov.dluhc.printapi.dto.SupportingInformationFormat as SupportingInformationFormatDto
import uk.gov.dluhc.printapi.models.CertificateLanguage as CertificateLanguageApi
import uk.gov.dluhc.printapi.models.SourceType as SourceTypeApi
import uk.gov.dluhc.printapi.models.SupportingInformationFormat as SupportingInformationFormatApi

@ExtendWith(MockitoExtension::class)
class GenerateAnonymousElectorDocumentMapperTest {

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var supportingInformationFormatMapper: SupportingInformationFormatMapper

    @InjectMocks
    private lateinit var mapper: GenerateAnonymousElectorDocumentMapperImpl

    @Test
    fun `should map to GenerateAnonymousElectorDocumentDto DTO given API Request`() {
        // Given
        val userId = aValidUserId()
        val apiRequest = buildGenerateAnonymousElectorDocumentRequest(
            sourceType = SourceTypeApi.VOTER_MINUS_CARD,
            certificateLanguage = CertificateLanguageApi.EN,
            supportingInformationFormat = SupportingInformationFormatApi.STANDARD
        )

        given(sourceTypeMapper.mapApiToDto(any())).willReturn(SourceTypeDto.VOTER_CARD)
        given(certificateLanguageMapper.mapApiToDto(any())).willReturn(CertificateLanguageDto.EN)
        given(supportingInformationFormatMapper.mapApiToDto(any())).willReturn(SupportingInformationFormatDto.STANDARD)

        val expected = GenerateAnonymousElectorDocumentDto(
            gssCode = apiRequest.gssCode,
            sourceType = SourceTypeDto.VOTER_CARD,
            sourceReference = apiRequest.sourceReference,
            applicationReference = apiRequest.applicationReference,
            electoralRollNumber = apiRequest.electoralRollNumber,
            photoLocation = apiRequest.photoLocation,
            certificateLanguage = CertificateLanguageDto.EN,
            supportingInformationFormat = SupportingInformationFormatDto.STANDARD,
            firstName = apiRequest.firstName,
            middleNames = apiRequest.middleNames,
            surname = apiRequest.surname,
            email = apiRequest.email,
            phoneNumber = apiRequest.phoneNumber,
            address = with(apiRequest.address) {
                AddressDto(
                    property = property,
                    street = street,
                    town = town,
                    area = area,
                    locality = locality,
                    uprn = uprn,
                    postcode = postcode
                )
            },
            userId = userId
        )

        // When
        val actual = mapper.toGenerateAnonymousElectorDocumentDto(apiRequest, userId)

        // Then
        verify(sourceTypeMapper).mapApiToDto(SourceTypeApi.VOTER_MINUS_CARD)
        verify(certificateLanguageMapper).mapApiToDto(CertificateLanguageApi.EN)
        verify(supportingInformationFormatMapper).mapApiToDto(SupportingInformationFormatApi.STANDARD)
        assertThat(actual).isEqualTo(expected)
    }
}
