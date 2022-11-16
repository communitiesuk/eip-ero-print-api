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
import uk.gov.dluhc.printapi.mapper.CertificateToPrintRequestMapper
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestsFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
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
        val currentPrintRequest = buildPrintRequest(batchId = batchId, requestId = requestId, photoLocationArn = photoArn)
        val certificate = buildCertificate(
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
        verify(filenameFactory).createPrintRequestsFilename(batchId, 1)
        verify(photoLocationFactory).create(batchId, requestId, photoArn)
        verify(certificateToPrintRequestMapper).map(certificate, currentPrintRequest, zipPath)
        assertThat(fileDetails.printRequestsFilename).isEqualTo(psvFilename)
        assertThat(fileDetails.photoLocations).containsExactly(photoLocation)
        assertThat(fileDetails.printRequests).containsExactly(printRequest)
    }
}
