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
        val deliveryId1 = certificate1.printRequests[0].delivery!!.id!!
        val deliveryId2 = certificate2.printRequests[0].delivery!!.id!!
        val deliveryId3 = certificate3.printRequests[0].delivery!!.id!!
        val deliveryId4 = certificate4.printRequests[0].delivery!!.id!!
        TestLogAppender.reset()

        // When
        initialRetentionPeriodDataRemovalJob.removeVoterCardInitialRetentionPeriodData()

        // Then
        assertThat(certificateRepository.findById(certificate1.id!!).get()).initialRetentionPeriodDataIsRemoved()
        assertThat(certificateRepository.findById(certificate2.id!!).get()).initialRetentionPeriodDataIsRemoved()
        assertThat(certificateRepository.findById(certificate3.id!!).get()).hasInitialRetentionPeriodData()
        assertThat(certificateRepository.findById(certificate4.id!!).get()).hasInitialRetentionPeriodData()
        assertThat(deliveryRepository.findById(deliveryId1)).isEmpty
        assertThat(deliveryRepository.findById(deliveryId2)).isEmpty
        assertThat(deliveryRepository.findById(deliveryId3)).isNotEmpty
        assertThat(deliveryRepository.findById(deliveryId4)).isNotEmpty
        assertThat(TestLogAppender.hasLog("Removed initial retention period data from certificate with sourceReference ${certificate1.sourceReference}", Level.INFO)).isTrue
        assertThat(TestLogAppender.hasLog("Removed initial retention period data from certificate with sourceReference ${certificate2.sourceReference}", Level.INFO)).isTrue
    }
}
