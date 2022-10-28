package uk.gov.dluhc.printapi.testsupport.testdata

import java.util.UUID

fun aBatchId() = UUID.randomUUID().toString().replace("-", "")

fun aPrintRequestsFilename() = "05372cf5339447b39f98b248c2217b9f-20221018112232123-10.psv"

fun aZipFilename() = "05372cf5339447b39f98b248c2217b9f-20221018112232123-10.zip"

fun anSftpPath() = "/home/valtech/dev/${aZipFilename()}"

fun anInputStream() = "Some input stream".byteInputStream()
