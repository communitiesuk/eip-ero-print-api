package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.exception.CertificateNotFoundException
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.SourceType.VOTER_MINUS_CARD
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildApplicationRemovedMessage
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
internal class CertificateDataRetentionServiceTest {

    @Mock
    private lateinit var certificateRemovalDateResolver: CertificateRemovalDateResolver

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @InjectMocks
    private lateinit var certificateDataRetentionService: CertificateDataRetentionService

    @Test
    fun `should handle source application removed`() {
        // Given
        val message = buildApplicationRemovedMessage()
        val certificate = buildCertificate()
        val expectedRemovalDate = LocalDate.of(2023, 1, 1)
        given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
        given(certificateRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(
            certificate
        )
        given(certificateRemovalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(any(), any())).willReturn(
            expectedRemovalDate
        )

        // When
        certificateDataRetentionService.handleSourceApplicationRemoved(message)

        // Then
        assertThat(certificate.initialRetentionRemovalDate).isEqualTo(expectedRemovalDate)
        verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
        verify(certificateRepository).findByGssCodeAndSourceTypeAndSourceReference(
            message.gssCode,
            VOTER_CARD,
            message.sourceReference
        )
        verify(certificateRepository).save(certificate)
    }

    @Test
    fun `should throw exception when certificate doesn't exist`() {
        // Given
        val message = buildApplicationRemovedMessage(
            sourceReference = "63774ff4bb4e7049b67182d9",
            sourceType = VOTER_MINUS_CARD
        )
        given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
        given(certificateRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(null)

        // When
        val error = catchThrowableOfType(
            { certificateDataRetentionService.handleSourceApplicationRemoved(message) },
            CertificateNotFoundException::class.java
        )

        // Then
        verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
        verify(certificateRepository).findByGssCodeAndSourceTypeAndSourceReference(
            message.gssCode,
            VOTER_CARD,
            message.sourceReference
        )
        assertThat(error)
            .isNotNull
            .hasMessage("Certificate with sourceType = VOTER_CARD and sourceReference = 63774ff4bb4e7049b67182d9 not found")
    }
}
