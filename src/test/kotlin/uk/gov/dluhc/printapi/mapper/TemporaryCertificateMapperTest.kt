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
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificateStatus
import uk.gov.dluhc.printapi.service.IdFactory
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates.aTemplateFilename
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class TemporaryCertificateMapperTest {

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var certificateLanguageMapper: CertificateLanguageMapper

    @Mock
    private lateinit var idFactory: IdFactory

    @InjectMocks
    private lateinit var mapper: TemporaryCertificateMapperImpl

    @Test
    fun `should map send application to print message for an English certificate to print details`() {
        // Given
        val ero = buildEroDto(
            welshContactDetails = null
        )
        val certificateRequest = buildGenerateTemporaryCertificateDto()
        val templateName = aTemplateFilename()
        val certificateNumber = aValidVacNumber()
        given(sourceTypeMapper.mapDtoToEntity(any())).willReturn(SourceType.VOTER_CARD)
        given(certificateLanguageMapper.mapDtoToEntity(any())).willReturn(CertificateLanguage.EN)
        given(idFactory.vacNumber()).willReturn(certificateNumber)

        val expected = with(certificateRequest) {
            TemporaryCertificate(
                id = null,
                certificateNumber = certificateNumber,
                gssCode = gssCode,
                sourceType = SourceType.VOTER_CARD,
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                certificateTemplateFilename = templateName,
                issuingAuthority = ero.englishContactDetails.name,
                issuingAuthorityCy = null,
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = CertificateLanguage.EN,
                photoLocationArn = photoLocation,
                issueDate = LocalDate.now(),
                validOnDate = validOnDate,
                statusHistory = mutableListOf(
                    TemporaryCertificateStatus()
                        .apply {
                            status = TemporaryCertificateStatus.Status.GENERATED
                            userId = certificateRequest.userId
                        }
                ),
                userId = userId
            )
        }

        // When
        val actual = mapper.toTemporaryCertificate(certificateRequest, ero, templateName)

        // Then
        verify(sourceTypeMapper).mapDtoToEntity(certificateRequest.sourceType)
        verify(certificateLanguageMapper).mapDtoToEntity(certificateRequest.certificateLanguage)
        verify(idFactory).vacNumber()
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
