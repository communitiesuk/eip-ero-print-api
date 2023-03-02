package uk.gov.dluhc.printapi.jobs

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import java.time.LocalDate
import java.util.UUID

internal class FinalRetentionPeriodDataRemovalJobIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var testPrintRequestRepository: TestPrintRequestRepository

    @Test
    fun `should remove voter card final retention period data`() {
        // Given
        val certificate1 = buildCertificate(finalRetentionRemovalDate = LocalDate.now().minusDays(1))
        val certificate2 = buildCertificate(finalRetentionRemovalDate = LocalDate.now().minusDays(1))
        val certificate3 = buildCertificate(finalRetentionRemovalDate = LocalDate.now())
        val certificate4 = buildCertificate(finalRetentionRemovalDate = LocalDate.now().plusDays(1))
        certificateRepository.saveAll(listOf(certificate1, certificate2, certificate3, certificate4))
        TestLogAppender.reset()

        // When
        finalRetentionPeriodDataRemovalJob.removeVoterCardFinalRetentionPeriodData()

        // Then
        assertThat(certificateRepository.findById(certificate1.id!!)).isEmpty
        assertThat(certificateRepository.findById(certificate2.id!!)).isEmpty
        assertThat(certificateRepository.findById(certificate3.id!!)).isNotEmpty
        assertThat(certificateRepository.findById(certificate4.id!!)).isNotEmpty
        assertThat(testPrintRequestRepository.findById(certificate1.printRequests[0].id!!)).isEmpty
        assertThat(testPrintRequestRepository.findById(certificate2.printRequests[0].id!!)).isEmpty
        assertThat(testPrintRequestRepository.findById(certificate3.printRequests[0].id!!)).isNotEmpty
        assertThat(testPrintRequestRepository.findById(certificate4.printRequests[0].id!!)).isNotEmpty
        assertThat(TestLogAppender.hasLog("Removed remaining data after final retention period from certificate with sourceReference ${certificate1.sourceReference}", Level.INFO)).isTrue
        assertThat(TestLogAppender.hasLog("Removed remaining data after final retention period from certificate with sourceReference ${certificate2.sourceReference}", Level.INFO)).isTrue
    }
}

@Repository
interface TestPrintRequestRepository : JpaRepository<PrintRequest, UUID>
