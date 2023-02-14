package uk.gov.dluhc.printapi.jobs

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import java.time.LocalDate

internal class InitialRetentionPeriodDataRemovalJobIntegrationTest : IntegrationTest() {

    @Test
    fun `should remove voter card initial retention period data`() {
        // Given
        val certificate1 = buildCertificate(initialRetentionRemovalDate = LocalDate.now().minusDays(1))
        val certificate2 = buildCertificate(initialRetentionRemovalDate = LocalDate.now().minusDays(1))
        val certificate3 = buildCertificate(initialRetentionRemovalDate = LocalDate.now())
        val certificate4 = buildCertificate(initialRetentionRemovalDate = LocalDate.now().plusDays(1))
        certificateRepository.saveAll(listOf(certificate1, certificate2, certificate3, certificate4))
        TestLogAppender.reset()

        // When
        initialRetentionPeriodDataRemovalJob.removeVoterCardInitialRetentionPeriodData()

        // Then
        assertThat(certificateRepository.findById(certificate1.id!!).get()).doesNotHaveInitialRetentionPeriodData()
        assertThat(certificateRepository.findById(certificate2.id!!).get()).doesNotHaveInitialRetentionPeriodData()
        assertThat(certificateRepository.findById(certificate3.id!!).get()).hasInitialRetentionPeriodData()
        assertThat(certificateRepository.findById(certificate4.id!!).get()).hasInitialRetentionPeriodData()

        assertThat(TestLogAppender.hasLog("Removed initial retention period data from certificate with sourceReference ${certificate1.sourceReference}", Level.INFO)).isTrue
        assertThat(TestLogAppender.hasLog("Removed initial retention period data from certificate with sourceReference ${certificate2.sourceReference}", Level.INFO)).isTrue
    }
}
