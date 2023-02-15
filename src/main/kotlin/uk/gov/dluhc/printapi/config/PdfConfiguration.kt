package uk.gov.dluhc.printapi.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.service.pdf.ElectorDocumentPdfTemplateDetailsFactory
import uk.gov.dluhc.printapi.service.pdf.ExplainerPdfService
import uk.gov.dluhc.printapi.service.pdf.ExplainerPdfTemplateDetailsFactory
import uk.gov.dluhc.printapi.service.pdf.PdfFactory

@Configuration
class PdfConfiguration {

    @Bean("temporaryCertificateExplainerPdfTemplateDetailsFactory")
    fun temporaryCertificateExplainerPdfTemplateDetailsFactory(
        explainerPdfTemplateProperties: TemporaryCertificateExplainerPdfTemplateProperties,
    ): ExplainerPdfTemplateDetailsFactory =
        ExplainerPdfTemplateDetailsFactory(explainerPdfTemplateProperties) {
            eroId: String, gssCode: String ->
            "Temporary certificate explainer document not found for eroId $eroId and gssCode $gssCode"
        }

    @Bean("temporaryCertificateElectorDocumentPdfTemplateDetailsFactory")
    fun temporaryCertificateElectorDocumentPdfTemplateDetailsFactory(
        s3Client: S3Client,
        pdfTemplateProperties: TemporaryCertificatePdfTemplateProperties
    ): ElectorDocumentPdfTemplateDetailsFactory =
        ElectorDocumentPdfTemplateDetailsFactory(s3Client, pdfTemplateProperties)

    @Bean("temporaryCertificateExplainerExplainerPdfService")
    fun temporaryCertificateExplainerExplainerPdfService(
        eroClient: ElectoralRegistrationOfficeManagementApiClient,
        pdfFactory: PdfFactory,
        @Qualifier("temporaryCertificateExplainerPdfTemplateDetailsFactory") explainerPdfTemplateDetailsFactory: ExplainerPdfTemplateDetailsFactory
    ): ExplainerPdfService =
        ExplainerPdfService(eroClient, explainerPdfTemplateDetailsFactory, pdfFactory)

    @Bean("anonymousElectorDocumentExplainerPdfTemplateDetailsFactory")
    fun anonymousElectorDocumentExplainerPdfTemplateDetailsFactory(
        explainerPdfTemplateProperties: AnonymousElectorDocumentExplainerPdfTemplateProperties,
    ): ExplainerPdfTemplateDetailsFactory =
        ExplainerPdfTemplateDetailsFactory(explainerPdfTemplateProperties) {
            eroId: String, gssCode: String ->
            "Anonymous Elector Document explainer document not found for eroId $eroId and gssCode $gssCode"
        }

    @Bean("anonymousElectorDocumentExplainerPdfService")
    fun anonymousElectorDocumentExplainerPdfService(
        eroClient: ElectoralRegistrationOfficeManagementApiClient,
        pdfFactory: PdfFactory,
        @Qualifier("anonymousElectorDocumentExplainerPdfTemplateDetailsFactory") explainerPdfTemplateDetailsFactory: ExplainerPdfTemplateDetailsFactory
    ): ExplainerPdfService =
        ExplainerPdfService(eroClient, explainerPdfTemplateDetailsFactory, pdfFactory)
}
