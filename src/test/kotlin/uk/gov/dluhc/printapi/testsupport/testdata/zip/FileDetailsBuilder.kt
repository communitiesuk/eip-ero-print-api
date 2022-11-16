package uk.gov.dluhc.printapi.testsupport.testdata.zip

import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.service.FileDetails
import uk.gov.dluhc.printapi.service.PhotoLocation
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestsFilename
import uk.gov.dluhc.printapi.testsupport.testdata.model.aPrintRequestList

fun buildFileDetails(
    printRequestsFilename: String = aValidPrintRequestsFilename(),
    printRequests: List<PrintRequest> = aPrintRequestList(),
    photoLocations: List<PhotoLocation> = aPhotoLocationList()
) = FileDetails(printRequestsFilename, printRequests, photoLocations)

fun aFileDetails(): FileDetails = buildFileDetails()
