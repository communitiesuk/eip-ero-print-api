package uk.gov.dluhc.printapi.messaging.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.service.FilenameFactory
import uk.gov.dluhc.printapi.service.PrintFileDetailsFactory
import uk.gov.dluhc.printapi.service.SftpInputStreamProvider
import uk.gov.dluhc.printapi.service.SftpService
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidInputStream
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSftpPath
import uk.gov.dluhc.printapi.testsupport.testdata.aValidZipFilename
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aFileDetails

@ExtendWith(MockitoExtension::class)
internal class ProcessPrintBatchServiceTest {
    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Mock
    private lateinit var printFileDetailsFactory: PrintFileDetailsFactory

    @Mock
    private lateinit var sftpZipInputStreamProvider: SftpInputStreamProvider

    @Mock
    private lateinit var filenameFactory: FilenameFactory

    @Mock
    private lateinit var sftpService: SftpService

    @InjectMocks
    private lateinit var processPrintBatchService: ProcessPrintBatchService

    @Test
    fun `should send file to SFTP`() {
        // Given
        val batchId = aValidBatchId()
        val printRequests = listOf(
            buildPrintRequest(
                batchId = batchId,
                printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.ASSIGNED_TO_BATCH))
            )
        )
        val certificates = listOf(buildCertificate(printRequests = printRequests))
        val printRequestCount = 1
        val fileDetails = aFileDetails()
        val sftpInputStream = aValidInputStream()
        val zipFilename = aValidZipFilename()
        val sftpPath = aValidSftpPath()
        given(certificateRepository.findByPrintRequestsBatchId(any())).willReturn(certificates)
        given(printFileDetailsFactory.createFileDetailsFromCertificates(any(), any())).willReturn(fileDetails)
        given(sftpZipInputStreamProvider.createSftpInputStream(any())).willReturn(sftpInputStream)
        given(filenameFactory.createZipFilename(any(), any())).willReturn(zipFilename)
        given(sftpService.sendFile(any(), any())).willReturn(sftpPath)

        // When
        processPrintBatchService.processBatch(batchId, printRequestCount)

        // Then
        verify(certificateRepository).findByPrintRequestsBatchId(batchId)
        verify(printFileDetailsFactory).createFileDetailsFromCertificates(batchId, certificates)
        verify(sftpZipInputStreamProvider).createSftpInputStream(fileDetails)
        verify(filenameFactory).createZipFilename(batchId, certificates)
        verify(sftpService).sendFile(sftpInputStream, zipFilename)
    }

    @Test
    fun `should raise exception when no certificates found for provided batchId`() {
        // Given
        val batchId = "4143d442a2424740afa3ce5eae630aad"
        val certificates = emptyList<Certificate>()
        val printRequestCount = null
        given(certificateRepository.findByPrintRequestsBatchId(any())).willReturn(certificates)

        // When
        val error = catchThrowable { processPrintBatchService.processBatch(batchId, printRequestCount) }

        // Then
        verify(certificateRepository).findByPrintRequestsBatchId(batchId)
        assertThat(error).hasMessage("Found 0 certificates for batchId = 4143d442a2424740afa3ce5eae630aad and status = ASSIGNED_TO_BATCH")
    }

    @Test
    fun `should raise exception when no certificates found for provided batchId with expected print request count provided`() {
        // Given
        val batchId = "4143d442a2424740afa3ce5eae630aae"
        val certificates = emptyList<Certificate>()
        val printRequestCount = 1
        given(certificateRepository.findByPrintRequestsBatchId(any())).willReturn(certificates)

        // When
        val error = catchThrowable { processPrintBatchService.processBatch(batchId, printRequestCount) }

        // Then
        verify(certificateRepository).findByPrintRequestsBatchId(batchId)
        assertThat(error).hasMessage("Found 0 of 1 certificates for batchId = 4143d442a2424740afa3ce5eae630aae and status = ASSIGNED_TO_BATCH")
    }

    @Test
    fun `should raise exception when print requests found is not what's expected`() {
        // Given
        val batchId = "4143d442a2424740afa3ce5eae630aaf"
        val printRequests = listOf(
            buildPrintRequest(
                batchId = batchId,
                printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.ASSIGNED_TO_BATCH))
            )
        )
        val certificates = listOf(buildCertificate(printRequests = printRequests))
        val printRequestCount = 2
        given(certificateRepository.findByPrintRequestsBatchId(any())).willReturn(certificates)

        // When
        val error = catchThrowable { processPrintBatchService.processBatch(batchId, printRequestCount) }

        // Then
        verify(certificateRepository).findByPrintRequestsBatchId(batchId)
        assertThat(error).hasMessage("Found 1 of 2 certificates for batchId = 4143d442a2424740afa3ce5eae630aaf and status = ASSIGNED_TO_BATCH")
    }
}
