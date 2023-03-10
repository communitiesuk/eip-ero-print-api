package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.repository.CertificateRemovalSummary
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.util.UUID

fun buildCertificateRemovalSummary(
    id: UUID? = UUID.randomUUID(),
    photoLocationArn: String = aPhotoArn()
): CertificateRemovalSummary =
    CertificateRemovalSummary(id = id, photoLocationArn = photoLocationArn)
