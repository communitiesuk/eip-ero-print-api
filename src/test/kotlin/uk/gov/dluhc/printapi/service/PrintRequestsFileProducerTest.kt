package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintRequest
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.Date

internal class PrintRequestsFileProducerTest {

    private val printRequestsFileProducer = PrintRequestsFileProducer()

    @Test
    fun `should write file to stream given print request with full details`() {
        // Given
        val outputStream = ByteArrayOutputStream()
        val printRequests = listOf(
            buildPrintRequest(
                requestId = "627ab400-5ae2-4cc5-9e91-5c050e43e4c1",
                issuingAuthorityEn = "Lake Deedra",
                issuingAuthorityCy = "Bradtketon",
                issueDate = "2022-06-01",
                suggestedExpiryDate = "2032-06-01",
                requestDateTime = Date(Instant.parse("2022-06-01T12:23:03.000Z").toEpochMilli()),
                cardFirstname = "Otelia",
                cardMiddlenames = "Shamika",
                cardSurname = "Ziemann",
                cardVersion = "1",
                cardNumber = "3987",
                certificateLanguage = PrintRequest.CertificateLanguage.EN,
                certificateFormat = PrintRequest.CertificateFormat.STANDARD,
                deliveryOption = PrintRequest.DeliveryOption.STANDARD,
                photo = "8a53a30ac9bae2ebb9b1239b.png",
                deliveryName = "Len Hessel DDS",
                deliveryProperty = "586",
                deliveryLocality = "Velva Square",
                deliveryTown = "East Eileneside",
                deliveryArea = "Mississippi",
                deliveryStreet = "Shon Flat",
                deliveryPostcode = "WR9 8JN",
                eroNameEn = "Lake Deedra",
                eroPhoneNumberEn = "07323 256949",
                eroEmailAddressEn = "contact@Lake-Deedra.gov.uk",
                eroWebsiteEn = "https://Lake-Deedra.gov.uk",
                eroDeliveryStreetEn = "Fidela Road",
                eroDeliveryPropertyEn = "44181",
                eroDeliveryLocalityEn = "David Orchard",
                eroDeliveryTownEn = "East Roberto",
                eroDeliveryAreaEn = "Nebraska",
                eroDeliveryPostcodeEn = "SS8Y 5RY",
                eroNameCy = "North Brittport",
                eroPhoneNumberCy = "07902 544470",
                eroEmailAddressCy = "contact@North-Brittport.gov.uk",
                eroWebsiteCy = "https://North-Brittport.gov.uk",
                eroDeliveryStreetCy = "Alexander Harbor",
                eroDeliverypPropertyCy = "3587",
                eroDeliveryLocalityCy = "Maggio Ways",
                eroDeliveryTownCy = "South Addieburgh",
                erodDeliveryAreaCy = "Oregon",
                eroDeliveryPostcodeCy = "WE7B 7FJ"
            )
        )

        // When
        printRequestsFileProducer.writeFileToStream(outputStream, printRequests)

        // Then
        val fileContents = String(outputStream.toByteArray())
        assertThat(fileContents).isEqualTo(
            """
  requestId|issuingAuthorityEn|issuingAuthorityCy|issueDate|suggestedExpiryDate|requestDateTime|cardFirstname|cardMiddlenames|cardSurname|cardVersion|cardNumber|certificateLanguage|certificateFormat|deliveryOption|photo|deliveryName|deliveryStreet|deliverypProperty|deliveryLocality|deliveryTown|deliveryArea|deliveryPostcode|eroNameEn|eroPhoneNumberEn|eroEmailAddressEn|eroWebsiteEn|eroDeliveryStreetEn|eroDeliveryPropertyEn|eroDeliveryLocalityEn|eroDeliveryTownEn|eroDeliveryAreaEn|eroDeliveryPostcodeEn|eroNameCy|eroPhoneNumberCy|eroEmailAddressCy|eroWebsiteCy|eroDeliveryStreetCy|eroDeliverypPropertyCy|eroDeliveryLocalityCy|eroDeliveryTownCy|erodDeliveryAreaCy|eroDeliveryPostcodeCy
  627ab400-5ae2-4cc5-9e91-5c050e43e4c1|Lake Deedra|Bradtketon|2022-06-01|2032-06-01|2022-06-01T12:23:03.000Z|Otelia|Shamika|Ziemann|1|3987|en|standard|standard|8a53a30ac9bae2ebb9b1239b.png|Len Hessel DDS|Shon Flat|586|Velva Square|East Eileneside|Mississippi|WR9 8JN|Lake Deedra|07323 256949|contact@Lake-Deedra.gov.uk|https://Lake-Deedra.gov.uk|Fidela Road|44181|David Orchard|East Roberto|Nebraska|SS8Y 5RY|North Brittport|07902 544470|contact@North-Brittport.gov.uk|https://North-Brittport.gov.uk|Alexander Harbor|3587|Maggio Ways|South Addieburgh|Oregon|WE7B 7FJ

            """.trimIndent()
        )
    }

    @Test
    fun `should write file to stream given print request with optional properties missing`() {
        // Given
        val outputStream = ByteArrayOutputStream()
        val printRequests = listOf(
            buildPrintRequest(
                requestId = "627ab400-5ae2-4cc5-9e91-5c050e43e4c1",
                issuingAuthorityEn = "Lake Deedra",
                issuingAuthorityCy = "Bradtketon",
                issueDate = "2022-06-01",
                suggestedExpiryDate = "2032-06-01",
                requestDateTime = Date(Instant.parse("2022-06-01T12:23:03.000Z").toEpochMilli()),
                cardFirstname = "Otelia",
                cardMiddlenames = "Shamika",
                cardSurname = "Ziemann",
                cardVersion = "1",
                cardNumber = "3987",
                certificateLanguage = PrintRequest.CertificateLanguage.EN,
                certificateFormat = PrintRequest.CertificateFormat.STANDARD,
                deliveryOption = PrintRequest.DeliveryOption.STANDARD,
                photo = "8a53a30ac9bae2ebb9b1239b.png",
                deliveryName = "Len Hessel DDS",
                deliveryProperty = null,
                deliveryLocality = null,
                deliveryTown = null,
                deliveryArea = null,
                deliveryStreet = "Shon Flat",
                deliveryPostcode = "WR9 8JN",
                eroNameEn = "Lake Deedra",
                eroPhoneNumberEn = "07323 256949",
                eroEmailAddressEn = "contact@Lake-Deedra.gov.uk",
                eroWebsiteEn = "https://Lake-Deedra.gov.uk",
                eroDeliveryStreetEn = "Fidela Road",
                eroDeliveryPropertyEn = "44181",
                eroDeliveryLocalityEn = "David Orchard",
                eroDeliveryTownEn = "East Roberto",
                eroDeliveryAreaEn = "Nebraska",
                eroDeliveryPostcodeEn = "SS8Y 5RY",
                eroNameCy = "North Brittport",
                eroPhoneNumberCy = "07902 544470",
                eroEmailAddressCy = "contact@North-Brittport.gov.uk",
                eroWebsiteCy = "https://North-Brittport.gov.uk",
                eroDeliveryStreetCy = "Alexander Harbor",
                eroDeliverypPropertyCy = "3587",
                eroDeliveryLocalityCy = "Maggio Ways",
                eroDeliveryTownCy = "South Addieburgh",
                erodDeliveryAreaCy = "Oregon",
                eroDeliveryPostcodeCy = "WE7B 7FJ"
            )
        )

        // When
        printRequestsFileProducer.writeFileToStream(outputStream, printRequests)

        // Then
        val fileContents = String(outputStream.toByteArray())
        assertThat(fileContents).isEqualTo(
            """
  requestId|issuingAuthorityEn|issuingAuthorityCy|issueDate|suggestedExpiryDate|requestDateTime|cardFirstname|cardMiddlenames|cardSurname|cardVersion|cardNumber|certificateLanguage|certificateFormat|deliveryOption|photo|deliveryName|deliveryStreet|deliverypProperty|deliveryLocality|deliveryTown|deliveryArea|deliveryPostcode|eroNameEn|eroPhoneNumberEn|eroEmailAddressEn|eroWebsiteEn|eroDeliveryStreetEn|eroDeliveryPropertyEn|eroDeliveryLocalityEn|eroDeliveryTownEn|eroDeliveryAreaEn|eroDeliveryPostcodeEn|eroNameCy|eroPhoneNumberCy|eroEmailAddressCy|eroWebsiteCy|eroDeliveryStreetCy|eroDeliverypPropertyCy|eroDeliveryLocalityCy|eroDeliveryTownCy|erodDeliveryAreaCy|eroDeliveryPostcodeCy
  627ab400-5ae2-4cc5-9e91-5c050e43e4c1|Lake Deedra|Bradtketon|2022-06-01|2032-06-01|2022-06-01T12:23:03.000Z|Otelia|Shamika|Ziemann|1|3987|en|standard|standard|8a53a30ac9bae2ebb9b1239b.png|Len Hessel DDS|Shon Flat|||||WR9 8JN|Lake Deedra|07323 256949|contact@Lake-Deedra.gov.uk|https://Lake-Deedra.gov.uk|Fidela Road|44181|David Orchard|East Roberto|Nebraska|SS8Y 5RY|North Brittport|07902 544470|contact@North-Brittport.gov.uk|https://North-Brittport.gov.uk|Alexander Harbor|3587|Maggio Ways|South Addieburgh|Oregon|WE7B 7FJ
  
            """.trimIndent()
        )
    }
}
