package uk.gov.dluhc.printapi.service

import com.opencsv.CSVWriter
import com.opencsv.ICSVWriter
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Optional

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
            ICSVWriter.NO_QUOTE_CHARACTER,
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
                "cardMiddlenames",
                "cardSurname",
                "cardVersion",
                "cardNumber",
                "certificateLanguage",
                "certificateFormat",
                "deliveryOption",
                "photo",
                "deliveryName",
                "deliveryStreet",
                "deliverypProperty",
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
                "eroDeliverypPropertyCy",
                "eroDeliveryLocalityCy",
                "eroDeliveryTownCy",
                "erodDeliveryAreaCy",
                "eroDeliveryPostcodeCy"
            )
        )
        printRequests.forEach { entry -> writer.writeNext(toStringArray(entry)) }
        osw.flush()
    }

    private fun toStringArray(printRequest: PrintRequest): Array<String> =
        with(printRequest) {
            arrayOf(
                requestId.toString(),
                issuingAuthorityEn,
                issuingAuthorityCy,
                issueDate.toString(),
                suggestedExpiryDate,
                requestDateTime.toInstant().atOffset(ZoneOffset.UTC).format(DATE_TIMESTAMP_FORMATTER),
                cardFirstname,
                Optional.ofNullable(cardMiddlenames).orElse(""),
                cardSurname,
                cardVersion,
                cardNumber,
                certificateLanguage.toString(),
                certificateFormat.toString(),
                deliveryOption.toString(),
                photo,
                deliveryName,
                deliveryStreet,
                Optional.ofNullable(deliverypProperty).orElse(""),
                Optional.ofNullable(deliveryLocality).orElse(""),
                Optional.ofNullable(deliveryTown).orElse(""),
                Optional.ofNullable(deliveryArea).orElse(""),
                Optional.ofNullable(deliveryPostcode).orElse(""),
                eroNameEn,
                eroPhoneNumberEn,
                eroEmailAddressEn,
                eroWebsiteEn,
                eroDeliveryStreetEn,
                Optional.ofNullable(eroDeliveryPropertyEn).orElse(""),
                Optional.ofNullable(eroDeliveryLocalityEn).orElse(""),
                Optional.ofNullable(eroDeliveryTownEn).orElse(""),
                Optional.ofNullable(eroDeliveryAreaEn).orElse(""),
                Optional.ofNullable(eroDeliveryPostcodeEn).orElse(""),
                Optional.ofNullable(eroNameCy).orElse(""),
                Optional.ofNullable(eroPhoneNumberCy).orElse(""),
                Optional.ofNullable(eroEmailAddressCy).orElse(""),
                Optional.ofNullable(eroWebsiteCy).orElse(""),
                Optional.ofNullable(eroDeliveryStreetCy).orElse(""),
                Optional.ofNullable(eroDeliverypPropertyCy).orElse(""),
                Optional.ofNullable(eroDeliveryLocalityCy).orElse(""),
                Optional.ofNullable(eroDeliveryTownCy).orElse(""),
                Optional.ofNullable(erodDeliveryAreaCy).orElse(""),
                Optional.ofNullable(eroDeliveryPostcodeCy).orElse("")
            )
        }

    companion object {
        private val DATE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    }
}
