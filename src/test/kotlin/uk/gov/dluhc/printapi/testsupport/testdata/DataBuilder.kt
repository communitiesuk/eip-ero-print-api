package uk.gov.dluhc.printapi.testsupport.testdata

import uk.gov.dluhc.printapi.domain.CertificateNumber
import java.util.UUID.randomUUID

fun aValidBatchId() = randomUUID().toString().replace("-", "")

fun aValidCertificateNumber() = CertificateNumber.create()

fun aGssCode() = "E99999999"

fun anotherGssCode() = "E11111111"

fun aValidPrintRequestsFilename() = "05372cf5339447b39f98b248c2217b9f-20221018112232123-10.psv"

fun aValidZipFilename() = "05372cf5339447b39f98b248c2217b9f-20221018112232123-10.zip"

fun aValidSftpPath() = "/home/valtech/dev/${aValidZipFilename()}"

fun aValidInputStream() = "Some input stream".byteInputStream()

fun aValidPrintResponseFileName() = "status-20221201171156568.json"
