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
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.dto.CertificateLanguage as CertificateLanguageDto
import uk.gov.dluhc.printapi.dto.SourceType as SourceTypeDto
import uk.gov.dluhc.printapi.models.CertificateLanguage as CertificateLanguageApi
import uk.gov.dluhc.printapi.models.SourceType as SourceTypeApi

@ExtendWith(MockitoExtension::class)
class GenerateTemporaryCertificateMapperTest {

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @InjectMocks
    private lateinit var mapper: GenerateTemporaryCertificateMapperImpl

    @Test
    fun `should map to GenerateTemporaryCertificate DTO given API Request`() {
        // Given
        val userId = aValidUserId()
        val apiRequest = buildGenerateTemporaryCertificateRequest(
            sourceType = SourceTypeApi.VOTER_MINUS_CARD,
            certificateLanguage = CertificateLanguageApi.EN
        )

        given(sourceTypeMapper.mapApiToDto(any())).willReturn(SourceTypeDto.VOTER_CARD)
        given(certificateLanguageMapper.mapApiToDto(any())).willReturn(CertificateLanguageDto.EN)

        val expected = buildGenerateTemporaryCertificateDto(
            gssCode = apiRequest.gssCode,
            sourceType = SourceTypeDto.VOTER_CARD,
            sourceReference = apiRequest.sourceReference,
            applicationReference = apiRequest.applicationReference,
            firstName = apiRequest.firstName,
            middleNames = apiRequest.middleNames,
            surname = apiRequest.surname,
            certificateLanguage = CertificateLanguageDto.EN,
            photoLocation = apiRequest.photoLocation,
            validOnDate = apiRequest.validOnDate,
            userId = userId
        )

        // When
        val actual = mapper.toGenerateTemporaryCertificateDto(apiRequest, userId)

        // Then
        verify(sourceTypeMapper).mapApiToDto(SourceTypeApi.VOTER_MINUS_CARD)
        verify(certificateLanguageMapper).mapApiToDto(CertificateLanguageApi.EN)
        assertThat(actual).isEqualTo(expected)
    }
}
