package uk.gov.dluhc.printapi.rds.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateNumber

internal class CertificateRepositoryTest : IntegrationTest() {

    @Test
    fun `should return Certificate given persisted Certificate`() {
        // Given
        val certificateNumber = aValidCertificateNumber()
        val gssCode = aGssCode()
        val certificate = Certificate(certificateNumber = certificateNumber, gssCode = gssCode)
        val expected = certificateRepository.save(certificate)

        // When
        val actual = certificateRepository.findById(expected.id!!)

        // Then
        assertThat(actual).isPresent
        assertThat(actual.get()).isEqualTo(expected)
    }
}
