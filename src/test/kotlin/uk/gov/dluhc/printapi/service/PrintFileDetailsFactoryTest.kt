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
import uk.gov.dluhc.printapi.mapper.PrintDetailsToPrintRequestMapper
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestsFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.aPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoZipPath
import uk.gov.dluhc.printapi.testsupport.testdata.zip.photoLocationBuilder

@ExtendWith(MockitoExtension::class)
internal class PrintFileDetailsFactoryTest {

    @Mock
    private lateinit var filenameFactory: FilenameFactory

    @Mock
    private lateinit var photoLocationFactory: PhotoLocationFactory

    @Mock
    private lateinit var printDetailsToPrintRequestMapper: PrintDetailsToPrintRequestMapper

    @InjectMocks
    private lateinit var printFileDetailsFactory: PrintFileDetailsFactory

    @Test
    fun `should create file details`() {
        // Given
        val batchId = aValidBatchId()
        val requestId = aValidRequestId()
        val photoArn = aPhotoArn()
        val printDetails = buildPrintDetails(batchId = batchId, requestId = requestId, photoLocation = photoArn)
        val printDetailsList = listOf(printDetails)
        val psvFilename = aValidPrintRequestsFilename()
        given(filenameFactory.createPrintRequestsFilename(any(), any())).willReturn(psvFilename)
        val zipPath = aPhotoZipPath()
        val photoLocation = photoLocationBuilder(zipPath = zipPath)
        given(photoLocationFactory.create(any(), any(), any())).willReturn(photoLocation)
        val printRequest = aPrintRequest()
        given(printDetailsToPrintRequestMapper.map(any(), any())).willReturn(printRequest)

        // When
        val fileDetails = printFileDetailsFactory.createFileDetails(batchId, printDetailsList)

        // Then
        verify(filenameFactory).createPrintRequestsFilename(batchId, 1)
        verify(photoLocationFactory).create(batchId, requestId, photoArn)
        verify(printDetailsToPrintRequestMapper).map(printDetails, zipPath)
        assertThat(fileDetails.printRequestsFilename).isEqualTo(psvFilename)
        assertThat(fileDetails.photoLocations).containsExactly(photoLocation)
        assertThat(fileDetails.printRequests).containsExactly(printRequest)
    }
}
