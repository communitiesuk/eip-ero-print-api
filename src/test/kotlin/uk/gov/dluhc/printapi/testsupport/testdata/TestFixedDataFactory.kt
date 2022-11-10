package uk.gov.dluhc.printapi.testsupport.testdata

import uk.gov.dluhc.printapi.database.entity.CertificateFormat
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status

fun aValidSourceType() = SourceType.VOTER_CARD

fun aValidCertificateStatus() = Status.PENDING_ASSIGNMENT_TO_BATCH

fun aValidCertificateLanguage() = CertificateLanguage.EN

fun aValidCertificateFormat() = CertificateFormat.STANDARD

fun aValidDeliveryClass(): DeliveryClass = DeliveryClass.STANDARD

fun aValidDeliveryMethod(): DeliveryMethod = DeliveryMethod.DELIVERY
