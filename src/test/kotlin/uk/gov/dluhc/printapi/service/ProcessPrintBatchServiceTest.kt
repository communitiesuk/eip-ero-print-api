package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidInputStream
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSftpPath
import uk.gov.dluhc.printapi.testsupport.testdata.aValidZipFilename
import uk.gov.dluhc.printapi.testsupport.testdata.rds.certificateBuilder
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
        val certificates = listOf(certificateBuilder())
        val fileDetails = aFileDetails()
        val sftpInputStream = aValidInputStream()
        val zipFilename = aValidZipFilename()
        val sftpPath = aValidSftpPath()
        given(certificateRepository.findByStatusAndPrintRequestsBatchId(any(), any())).willReturn(certificates)
        given(printFileDetailsFactory.createFileDetailsFromCertificates(any(), any())).willReturn(fileDetails)
        given(sftpZipInputStreamProvider.createSftpInputStream(any())).willReturn(sftpInputStream)
        given(filenameFactory.createZipFilename(any(), any())).willReturn(zipFilename)
        given(sftpService.sendFile(any(), any())).willReturn(sftpPath)

        // When
        processPrintBatchService.processBatch(batchId)

        // Then
        verify(certificateRepository).findByStatusAndPrintRequestsBatchId(ASSIGNED_TO_BATCH, batchId)
        verify(printFileDetailsFactory).createFileDetailsFromCertificates(batchId, certificates)
        verify(sftpZipInputStreamProvider).createSftpInputStream(fileDetails)
        verify(filenameFactory).createZipFilename(batchId, certificates.size)
        verify(sftpService).sendFile(sftpInputStream, zipFilename)
    }
}
