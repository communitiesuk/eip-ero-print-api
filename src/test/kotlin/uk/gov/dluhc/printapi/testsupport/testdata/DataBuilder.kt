package uk.gov.dluhc.printapi.testsupport.testdata

import java.util.UUID

fun aValidBatchId() = UUID.randomUUID().toString().replace("-", "")

fun aValidPrintRequestsFilename() = "05372cf5339447b39f98b248c2217b9f-20221018112232123-10.psv"

fun aValidZipFilename() = "05372cf5339447b39f98b248c2217b9f-20221018112232123-10.zip"

fun aValidSftpPath() = "/home/valtech/dev/${aValidZipFilename()}"

fun aValidInputStream() = "Some input stream".byteInputStream()
