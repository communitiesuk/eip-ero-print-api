package uk.gov.dluhc.printapi.jobs

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
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

    @Test
    fun `should remove anonymous elector document initial retention period data`() {
        // Given
        val aed1 = buildAnonymousElectorDocument(initialRetentionRemovalDate = LocalDate.now().minusDays(1))
        val aed2 = buildAnonymousElectorDocument(initialRetentionRemovalDate = LocalDate.now().minusDays(1))
        val aed3 = buildAnonymousElectorDocument(initialRetentionRemovalDate = LocalDate.now())
        val aed4 = buildAnonymousElectorDocument(initialRetentionRemovalDate = LocalDate.now().plusDays(1))
        anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2, aed3, aed4))
        val deliveryId1 = aed1.delivery!!.id!!
        val deliveryId2 = aed2.delivery!!.id!!
        val deliveryId3 = aed3.delivery!!.id!!
        val deliveryId4 = aed4.delivery!!.id!!
        val addressId1 = aed1.contactDetails?.address!!.id!!
        val addressId2 = aed2.contactDetails?.address!!.id!!
        val addressId3 = aed3.contactDetails?.address!!.id!!
        val addressId4 = aed4.contactDetails?.address!!.id!!
        TestLogAppender.reset()

        // When
        initialRetentionPeriodDataRemovalJob.removeVoterCardInitialRetentionPeriodData()

        // Then
        assertThat(anonymousElectorDocumentRepository.findById(aed1.id!!).get()).initialRetentionPeriodDataIsRemoved()
        assertThat(anonymousElectorDocumentRepository.findById(aed2.id!!).get()).initialRetentionPeriodDataIsRemoved()
        assertThat(anonymousElectorDocumentRepository.findById(aed3.id!!).get()).hasInitialRetentionPeriodData()
        assertThat(anonymousElectorDocumentRepository.findById(aed4.id!!).get()).hasInitialRetentionPeriodData()
        assertThat(deliveryRepository.findById(deliveryId1)).isEmpty
        assertThat(deliveryRepository.findById(deliveryId2)).isEmpty
        assertThat(deliveryRepository.findById(deliveryId3)).isNotEmpty
        assertThat(deliveryRepository.findById(deliveryId4)).isNotEmpty
        assertThat(addressRepository.findById(addressId1)).isEmpty
        assertThat(addressRepository.findById(addressId2)).isEmpty
        assertThat(addressRepository.findById(addressId3)).isNotEmpty
        assertThat(addressRepository.findById(addressId4)).isNotEmpty
        assertThat(TestLogAppender.hasLog("Removed initial retention period data from anonymous elector document with sourceReference ${aed1.sourceReference}", Level.INFO)).isTrue
        assertThat(TestLogAppender.hasLog("Removed initial retention period data from anonymous elector document with sourceReference ${aed2.sourceReference}", Level.INFO)).isTrue
    }
}
