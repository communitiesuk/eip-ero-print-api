package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateNumber

@ExtendWith(MockitoExtension::class)
internal class IdFactoryTest {

    @Mock
    private lateinit var certificateNumberGenerator: CertificateNumberGenerator

    @InjectMocks
    private lateinit var idFactory: IdFactory

    @Test
    fun `should generate requestId`() {
        // Given

        // When
        val requestId = idFactory.requestId()

        // Then
        assertThat(requestId).hasSize(24)
    }

    @Test
    fun `should generate unique requestId with each call`() {
        // Given
        val requestIds = mutableListOf<String>()

        // When
        repeat(100) {
            requestIds.add(idFactory.requestId())
        }

        // Then
        assertThat(requestIds).doesNotHaveDuplicates()
    }

    @Test
    fun `should generate vacNumber`() {
        // Given
        val expectedCertificateNumber = aValidCertificateNumber()
        given(certificateNumberGenerator.generateCertificateNumber()).willReturn(expectedCertificateNumber)

        // When
        val certificateNumber = idFactory.vacNumber()

        // Then
        assertThat(certificateNumber).isEqualTo(expectedCertificateNumber)
    }
}
