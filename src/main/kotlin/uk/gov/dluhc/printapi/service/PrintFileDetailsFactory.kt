package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.mapper.CertificateToPrintRequestMapper
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequest as EntityPrintRequest

@Component
class PrintFileDetailsFactory(
    private val filenameFactory: FilenameFactory,
    private val photoLocationFactory: PhotoLocationFactory,
    private val certificateToPrintRequestMapper: CertificateToPrintRequestMapper
) {

    fun createFileDetailsFromCertificates(batchId: String, certificates: List<Certificate>): FileDetails {
        val fileContents = createFromCertificates(certificates)
        return FileDetails(
            printRequestsFilename = filenameFactory.createPrintRequestsFilename(batchId, certificates),
            printRequests = fileContents.printRequests,
            photoLocations = fileContents.photoLocations
        )
    }

    private fun createFromCertificates(certificates: List<Certificate>): FileContents {
        val printRequests = mutableListOf<PrintRequest>()
        val photoLocations = mutableListOf<PhotoLocation>()
        certificates.forEach { certificate -> parseCertificate(certificate, printRequests, photoLocations) }
        return FileContents(printRequests, photoLocations)
    }

    private fun parseCertificate(
        certificate: Certificate,
        requests: MutableList<PrintRequest>,
        photos: MutableList<PhotoLocation>
    ) {
        certificate.getPrintRequestsByStatus(Status.ASSIGNED_TO_BATCH)
            .forEach { requestInBatch -> processPrintRequest(requestInBatch, certificate, requests, photos) }
    }

    private fun processPrintRequest(
        pendingPrintRequest: EntityPrintRequest,
        certificate: Certificate,
        requests: MutableList<PrintRequest>,
        photos: MutableList<PhotoLocation>
    ) {
        val photoLocation =
            photoLocationFactory.create(pendingPrintRequest.batchId!!, pendingPrintRequest.requestId!!, certificate.photoLocationArn!!)
        val printRequest = certificateToPrintRequestMapper.map(certificate, pendingPrintRequest, photoLocation.zipPath)
        requests.add(printRequest)
        photos.add(photoLocation)
    }
}

data class FileDetails(
    val printRequestsFilename: String,
    val printRequests: List<PrintRequest>,
    val photoLocations: List<PhotoLocation>
)

private class FileContents(
    val printRequests: List<PrintRequest>,
    val photoLocations: List<PhotoLocation>
)

data class PhotoLocation(
    val zipPath: String,
    val sourceBucket: String,
    val sourcePath: String
)
