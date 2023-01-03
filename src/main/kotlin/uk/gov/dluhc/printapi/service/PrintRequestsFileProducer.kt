package uk.gov.dluhc.printapi.service

import com.opencsv.CSVWriter
import com.opencsv.ICSVWriter
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.format.DateTimeFormatter

@Component
class PrintRequestsFileProducer {

    /**
     * Writes the print requests file to the output stream and leaves it open for further data
     * to be written to the stream.
     */
    fun writeFileToStream(outputStream: OutputStream, printRequests: List<PrintRequest>) {
        // avoiding `use` as need to keep the output stream open after writing this file
        val osw = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        val writer = CSVWriter(
            osw,
            '|',
            ICSVWriter.DEFAULT_QUOTE_CHARACTER,
            ICSVWriter.NO_ESCAPE_CHARACTER,
            ICSVWriter.DEFAULT_LINE_END
        )
        writer.writeNext(
            arrayOf(
                "requestId",
                "issuingAuthorityEn",
                "issuingAuthorityCy",
                "issueDate",
                "suggestedExpiryDate",
                "requestDateTime",
                "cardFirstname",
                "cardMiddleNames",
                "cardSurname",
                "cardVersion",
                "cardNumber",
                "certificateLanguage",
                "certificateFormat",
                "deliveryOption",
                "photo",
                "deliveryName",
                "deliveryStreet",
                "deliveryProperty",
                "deliveryLocality",
                "deliveryTown",
                "deliveryArea",
                "deliveryPostcode",
                "eroNameEn",
                "eroPhoneNumberEn",
                "eroEmailAddressEn",
                "eroWebsiteEn",
                "eroDeliveryStreetEn",
                "eroDeliveryPropertyEn",
                "eroDeliveryLocalityEn",
                "eroDeliveryTownEn",
                "eroDeliveryAreaEn",
                "eroDeliveryPostcodeEn",
                "eroNameCy",
                "eroPhoneNumberCy",
                "eroEmailAddressCy",
                "eroWebsiteCy",
                "eroDeliveryStreetCy",
                "eroDeliveryPropertyCy",
                "eroDeliveryLocalityCy",
                "eroDeliveryTownCy",
                "eroDeliveryAreaCy",
                "eroDeliveryPostcodeCy"
            )
        )
        printRequests.forEach { entry -> writer.writeNext(toStringArray(entry)) }
        osw.flush()
    }

    private fun toStringArray(printRequest: PrintRequest): Array<String> =
        with(printRequest) {
            arrayOf(
                requestId,
                issuingAuthorityEn,
                getWelshValue(issuingAuthorityCy, issuingAuthorityEn),
                issueDate.format(DATE_FORMATTER),
                suggestedExpiryDate.format(DATE_FORMATTER),
                requestDateTime.format(DATE_TIMESTAMP_FORMATTER),
                cardFirstname,
                cardMiddleNames ?: "",
                cardSurname,
                cardVersion,
                cardNumber,
                certificateLanguage.toString(),
                certificateFormat.toString(),
                deliveryOption.toString(),
                photo,
                deliveryName,
                deliveryStreet,
                deliveryProperty ?: "",
                deliveryLocality ?: "",
                deliveryTown ?: "",
                deliveryArea ?: "",
                deliveryPostcode,
                eroNameEn,
                eroPhoneNumberEn,
                eroEmailAddressEn,
                eroWebsiteEn,
                eroDeliveryStreetEn,
                eroDeliveryPropertyEn ?: "",
                eroDeliveryLocalityEn ?: "",
                eroDeliveryTownEn ?: "",
                eroDeliveryAreaEn ?: "",
                eroDeliveryPostcodeEn,
                getWelshValue(eroNameCy, eroNameEn),
                getWelshValue(eroPhoneNumberCy, eroPhoneNumberEn),
                getWelshValue(eroEmailAddressCy, eroEmailAddressEn),
                getWelshValue(eroWebsiteCy, eroWebsiteEn),
                getWelshValue(eroDeliveryStreetCy, eroDeliveryStreetEn),
                getWelshValue(eroDeliveryPropertyCy, eroDeliveryPropertyEn),
                getWelshValue(eroDeliveryLocalityCy, eroDeliveryLocalityEn),
                getWelshValue(eroDeliveryTownCy, eroDeliveryTownEn),
                getWelshValue(eroDeliveryAreaCy, eroDeliveryAreaEn),
                getWelshValue(eroDeliveryPostcodeCy, eroDeliveryPostcodeEn)
            )
        }

    private fun PrintRequest.getWelshValue(welshValue: String?, englishValue: String?): String {
        val isWelshAndWelshPropertiesMissing =
            certificateLanguage == PrintRequest.CertificateLanguage.CY && eroNameCy == null

        if (isWelshAndWelshPropertiesMissing) {
            return englishValue ?: ""
        }

        return welshValue ?: ""
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val DATE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    }
}
