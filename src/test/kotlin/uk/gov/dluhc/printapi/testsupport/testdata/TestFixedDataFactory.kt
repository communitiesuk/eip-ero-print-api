package uk.gov.dluhc.printapi.testsupport.testdata

import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificateStatus

fun aValidSourceType() = SourceType.VOTER_CARD

fun aValidCertificateStatus() = Status.PENDING_ASSIGNMENT_TO_BATCH

fun aDifferentValidCertificateStatus() = Status.DISPATCHED

fun aValidCertificateLanguage() = CertificateLanguage.EN

fun aValidSupportingInformationFormat() = SupportingInformationFormat.STANDARD

fun aValidDeliveryClass(): DeliveryClass = DeliveryClass.STANDARD

fun aValidDeliveryAddressType(): DeliveryAddressType = DeliveryAddressType.REGISTERED

fun aValidAddressFormat(): AddressFormat = AddressFormat.UK

fun aValidTemporaryCertificateTemplateFilename(): String = "temporary-certificate-template-en.pdf"

fun aValidTemporaryCertificateStatus() = TemporaryCertificateStatus.Status.GENERATED
