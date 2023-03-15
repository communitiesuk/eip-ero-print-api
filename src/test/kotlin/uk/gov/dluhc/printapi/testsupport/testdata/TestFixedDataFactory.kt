package uk.gov.dluhc.printapi.testsupport.testdata

import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificateStatus
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.dto.CertificateLanguage as CertificateLanguageDto
import uk.gov.dluhc.printapi.dto.SourceType as SourceTypeDto

fun aValidSourceType() = SourceTypeEntity.VOTER_CARD

fun anAnonymousElectorDocumentSourceType() = SourceTypeEntity.ANONYMOUS_ELECTOR_DOCUMENT

fun aValidCertificateStatus() = Status.PENDING_ASSIGNMENT_TO_BATCH

fun aDifferentValidCertificateStatus() = Status.DISPATCHED

fun aValidCertificateLanguage() = CertificateLanguageEntity.EN

fun aValidSupportingInformationFormat() = SupportingInformationFormat.STANDARD

fun aValidDeliveryClass(): DeliveryClass = DeliveryClass.STANDARD

fun aValidDeliveryAddressType(): DeliveryAddressType = DeliveryAddressType.REGISTERED

fun aValidAddressFormat(): AddressFormat = AddressFormat.UK

fun aValidTemporaryCertificateTemplateFilename(): String = "temporary-certificate-template-en.pdf"

fun aValidAnonymousElectorDocumentTemplateFilename(): String = "anonymous-elector-document-template-en.pdf"

fun aValidTemporaryCertificateStatus() = TemporaryCertificateStatus.Status.GENERATED

fun aValidAnonymousElectorDocumentStatus() = AnonymousElectorDocumentStatus.Status.GENERATED

fun aValidSourceTypeDto() = SourceTypeDto.VOTER_CARD

fun aValidCertificateLanguageDto() = CertificateLanguageDto.EN
