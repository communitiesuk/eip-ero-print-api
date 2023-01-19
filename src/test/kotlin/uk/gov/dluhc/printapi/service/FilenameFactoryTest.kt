package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal class FilenameFactoryTest {

    private lateinit var filenameFactory: FilenameFactory
    private val fixedClock = Clock.fixed(Instant.parse("2022-10-18T11:22:32.123Z"), ZoneId.of("UTC"))

    @BeforeEach
    fun setUp() {
        filenameFactory = FilenameFactory(fixedClock)
    }

    @Test
    fun `should create zip filename`() {
        // Given
        val batchId = "05372cf5339447b39f98b248c2217b9f"
        val count = 10
        val certificates = (1..count)
            .map { buildCertificate(batchId = batchId, status = Status.ASSIGNED_TO_BATCH) }

        // When
        val filename = filenameFactory.createZipFilename(batchId, certificates)

        // Then
        assertThat(filename).isEqualTo("05372cf5339447b39f98b248c2217b9f-20221018112232123-10.zip")
    }

    @Test
    fun `should create print requests filename`() {
        // Given
        val batchId = "49825273c8e64dd885886b74883b8bb3"
        val count = 19
        val certificates = (1..count)
            .map { buildCertificate(batchId = batchId, status = Status.ASSIGNED_TO_BATCH) }

        // When
        val filename = filenameFactory.createPrintRequestsFilename(batchId, certificates)

        // Then
        assertThat(filename).isEqualTo("49825273c8e64dd885886b74883b8bb3-20221018112232123-19.psv")
    }
}
