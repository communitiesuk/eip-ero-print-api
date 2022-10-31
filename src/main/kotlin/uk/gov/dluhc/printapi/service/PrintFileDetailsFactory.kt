package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.mapper.PrintDetailsToPrintRequestMapper
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest

@Component
class PrintFileDetailsFactory(
    private val filenameFactory: FilenameFactory,
    private val photoLocationFactory: PhotoLocationFactory,
    private val printDetailsToPrintRequestMapper: PrintDetailsToPrintRequestMapper
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
