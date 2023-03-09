package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.repository.CertificateRemovalSummary
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.util.UUID

fun buildCertificateRemovalSummary(
    id: UUID? = UUID.randomUUID(),
    applicationReference: String = aPhotoArn() // TODO EIP1-4307 - change to photoLocationArn
): CertificateRemovalSummary =
    CertificateRemovalSummary(id = id, applicationReference = applicationReference)
