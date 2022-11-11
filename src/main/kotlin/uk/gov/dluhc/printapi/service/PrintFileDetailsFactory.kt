package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.mapper.PrintDetailsToPrintRequestMapper
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.mapper.CertificateToPrintRequestMapper

@Component
class PrintFileDetailsFactory(
    private val filenameFactory: FilenameFactory,
    private val photoLocationFactory: PhotoLocationFactory,
    private val printDetailsToPrintRequestMapper: PrintDetailsToPrintRequestMapper,
    private val certificateToPrintRequestMapper: CertificateToPrintRequestMapper
) {

    fun createFileDetails(batchId: String, printList: List<PrintDetails>): FileDetails {
        val fileContents = createFrom(printList)
        return FileDetails(
            printRequestsFilename = filenameFactory.createPrintRequestsFilename(batchId, printList.size),
            printRequests = fileContents.printRequests,
            photoLocations = fileContents.photoLocations
        )
    }

    private fun createFrom(printList: List<PrintDetails>): FileContents {
        val printRequests = mutableListOf<PrintRequest>()
        val photoLocations = mutableListOf<PhotoLocation>()
        printList.forEach { printDetails -> parsePrintDetails(printDetails, printRequests, photoLocations) }
        return FileContents(printRequests, photoLocations)
    }

    private fun parsePrintDetails(
        details: PrintDetails,
        requests: MutableList<PrintRequest>,
        photos: MutableList<PhotoLocation>
    ) {
        val photoArn = details.photoLocation!!
        val photoLocation = photoLocationFactory.create(details.batchId!!, details.requestId!!, photoArn)
        val printRequest = printDetailsToPrintRequestMapper.map(details, photoLocation.zipPath)
        requests.add(printRequest)
        photos.add(photoLocation)
    }

    fun createFileDetailsFromCertificates(batchId: String, certificates: List<Certificate>): FileDetails {
        val fileContents = createFromCertificates(certificates)
        return FileDetails(
            printRequestsFilename = filenameFactory.createPrintRequestsFilename(batchId, certificates.size),
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
        val latestRequest = certificate.getCurrentPrintRequest()
        val photoArn = latestRequest.photoLocationArn!!
        val photoLocation = photoLocationFactory.create(latestRequest.batchId!!, latestRequest.requestId!!, photoArn)
        val printRequest = certificateToPrintRequestMapper.map(certificate, latestRequest, photoLocation.zipPath)
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
