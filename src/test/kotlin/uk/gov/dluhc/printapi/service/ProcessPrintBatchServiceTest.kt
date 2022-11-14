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
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidInputStream
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSftpPath
import uk.gov.dluhc.printapi.testsupport.testdata.aValidZipFilename
import uk.gov.dluhc.printapi.testsupport.testdata.entity.aPrintDetailsList
import uk.gov.dluhc.printapi.testsupport.testdata.rds.certificateBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aFileDetails

@ExtendWith(MockitoExtension::class)
internal class ProcessPrintBatchServiceTest {
    @Mock
    private lateinit var printDetailsRepository: PrintDetailsRepository

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
        val printList = aPrintDetailsList()
        val certificates = listOf(certificateBuilder())
        val fileDetails = aFileDetails()
        val sftpInputStream = aValidInputStream()
        val zipFilename = aValidZipFilename()
        val sftpPath = aValidSftpPath()
        given(printDetailsRepository.getAllByStatusAndBatchId(any(), any())).willReturn(printList)
        given(certificateRepository.findByStatusAndPrintRequestsBatchIdIs(any(), any())).willReturn(certificates)
        given(printFileDetailsFactory.createFileDetails(any(), any())).willReturn(fileDetails)
        given(printFileDetailsFactory.createFileDetailsFromCertificates(any(), any())).willReturn(fileDetails)
        given(sftpZipInputStreamProvider.createSftpInputStream(any())).willReturn(sftpInputStream)
        given(filenameFactory.createZipFilename(any(), any())).willReturn(zipFilename)
        given(sftpService.sendFile(any(), any())).willReturn(sftpPath)

        // When
        processPrintBatchService.processBatch(batchId)

        // Then
        verify(printDetailsRepository).getAllByStatusAndBatchId(ASSIGNED_TO_BATCH, batchId)
        verify(certificateRepository).findByStatusAndPrintRequestsBatchIdIs(ASSIGNED_TO_BATCH, batchId)
        verify(printFileDetailsFactory).createFileDetails(batchId, printList)
        verify(printFileDetailsFactory).createFileDetailsFromCertificates(batchId, certificates)
        verify(sftpZipInputStreamProvider).createSftpInputStream(fileDetails)
        verify(filenameFactory).createZipFilename(batchId, printList.size)
        verify(sftpService).sendFile(sftpInputStream, zipFilename)
    }
}
