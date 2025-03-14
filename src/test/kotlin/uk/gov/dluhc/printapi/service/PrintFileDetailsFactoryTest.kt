package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.mapper.CertificateToPrintRequestMapper
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestsFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.model.aPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoZipPath
import uk.gov.dluhc.printapi.testsupport.testdata.zip.buildPhotoLocation

@ExtendWith(MockitoExtension::class)
internal class PrintFileDetailsFactoryTest {

    @Mock
    private lateinit var filenameFactory: FilenameFactory

    @Mock
    private lateinit var photoLocationFactory: PhotoLocationFactory

    @Mock
    private lateinit var certificateToPrintRequestMapper: CertificateToPrintRequestMapper

    @InjectMocks
    private lateinit var printFileDetailsFactory: PrintFileDetailsFactory

    @Test
    fun `should create file details from certificates`() {
        // Given
        val batchId = aValidBatchId()
        val requestId = aValidRequestId()
        val photoArn = aPhotoArn()
        val currentPrintRequest = buildPrintRequest(
            batchId = batchId,
            printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.ASSIGNED_TO_BATCH)),
            requestId = requestId,
        )
        val certificate = buildCertificate(
            photoLocationArn = photoArn,
            printRequests = mutableListOf(currentPrintRequest)
        )
        val certificates = listOf(certificate)
        val psvFilename = aValidPrintRequestsFilename()
        given(filenameFactory.createPrintRequestsFilename(any(), any())).willReturn(psvFilename)
        val zipPath = aPhotoZipPath()
        val photoLocation = buildPhotoLocation(zipPath = zipPath)
        given(photoLocationFactory.create(any(), any(), any())).willReturn(photoLocation)
        val printRequest = aPrintRequest()
        given(certificateToPrintRequestMapper.map(any(), any(), any())).willReturn(printRequest)

        // When
        val fileDetails = printFileDetailsFactory.createFileDetailsFromCertificates(batchId, certificates)

        // Then
        verify(filenameFactory).createPrintRequestsFilename(batchId, certificates)
        verify(photoLocationFactory).create(batchId, requestId, photoArn)
        verify(certificateToPrintRequestMapper).map(certificate, currentPrintRequest, zipPath)
        assertThat(fileDetails.printRequestsFilename).isEqualTo(psvFilename)
        assertThat(fileDetails.photoLocations).containsExactly(photoLocation)
        assertThat(fileDetails.printRequests).containsExactly(printRequest)
    }

    @Test
    fun `should create file details from certificate with multiple print requests`() {
        // Given
        val batchId = aValidBatchId()
        val firstRequestId = aValidRequestId()
        val secondRequestId = aValidRequestId()
        val photoArn = aPhotoArn()
        val firstPrintRequest = buildPrintRequest(
            batchId = batchId,
            printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.ASSIGNED_TO_BATCH)),
            requestId = firstRequestId,
        )
        val secondPrintRequest = buildPrintRequest(
            batchId = batchId,
            printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.ASSIGNED_TO_BATCH)),
            requestId = secondRequestId,
        )
        val certificate = buildCertificate(
            photoLocationArn = photoArn,
            printRequests = mutableListOf(firstPrintRequest, secondPrintRequest)
        )
        val certificates = listOf(certificate)
        val psvFilename = aValidPrintRequestsFilename()
        given(filenameFactory.createPrintRequestsFilename(any(), any())).willReturn(psvFilename)
        val zipPath = aPhotoZipPath()
        val photoLocation = buildPhotoLocation(zipPath = zipPath)
        given(photoLocationFactory.create(any(), any(), any())).willReturn(photoLocation)
        val printRequest = aPrintRequest()
        given(certificateToPrintRequestMapper.map(any(), any(), any())).willReturn(printRequest)

        // When
        val fileDetails = printFileDetailsFactory.createFileDetailsFromCertificates(batchId, certificates)

        // Then
        verify(filenameFactory).createPrintRequestsFilename(batchId, certificates)
        verify(photoLocationFactory).create(batchId, firstRequestId, photoArn)
        verify(photoLocationFactory).create(batchId, secondRequestId, photoArn)
        verify(certificateToPrintRequestMapper).map(certificate, firstPrintRequest, zipPath)
        assertThat(fileDetails.printRequestsFilename).isEqualTo(psvFilename)
        assertThat(fileDetails.photoLocations).containsExactly(photoLocation, photoLocation)
        assertThat(fileDetails.printRequests).containsExactly(printRequest, printRequest)
    }

    @Test
    fun `should only include print requests with the given batch id in the file details`() {
        // Given
        val batchId = aValidBatchId()
        val anotherBatchId = aValidBatchId()
        val firstRequestId = aValidRequestId()
        val secondRequestId = aValidRequestId()
        val photoArn = aPhotoArn()
        val firstPrintRequest = buildPrintRequest(
            batchId = batchId,
            printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.ASSIGNED_TO_BATCH)),
            requestId = firstRequestId,
        )
        val secondPrintRequest = buildPrintRequest(
            batchId = anotherBatchId,
            printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.ASSIGNED_TO_BATCH)),
            requestId = secondRequestId,
        )
        val certificate = buildCertificate(
            photoLocationArn = photoArn,
            printRequests = mutableListOf(firstPrintRequest, secondPrintRequest)
        )
        val certificates = listOf(certificate)
        val psvFilename = aValidPrintRequestsFilename()
        given(filenameFactory.createPrintRequestsFilename(any(), any())).willReturn(psvFilename)
        val zipPath = aPhotoZipPath()
        val photoLocation = buildPhotoLocation(zipPath = zipPath)
        given(photoLocationFactory.create(any(), any(), any())).willReturn(photoLocation)
        val printRequest = aPrintRequest()
        given(certificateToPrintRequestMapper.map(any(), any(), any())).willReturn(printRequest)

        // When
        val fileDetails = printFileDetailsFactory.createFileDetailsFromCertificates(batchId, certificates)

        // Then
        verify(filenameFactory).createPrintRequestsFilename(batchId, certificates)
        verify(photoLocationFactory).create(batchId, firstRequestId, photoArn)
        verify(certificateToPrintRequestMapper).map(certificate, firstPrintRequest, zipPath)
        verifyNoMoreInteractions(photoLocationFactory, certificateToPrintRequestMapper)
        assertThat(fileDetails.printRequestsFilename).isEqualTo(psvFilename)
        assertThat(fileDetails.photoLocations).containsExactly(photoLocation)
        assertThat(fileDetails.printRequests).containsExactly(printRequest)
    }
}
