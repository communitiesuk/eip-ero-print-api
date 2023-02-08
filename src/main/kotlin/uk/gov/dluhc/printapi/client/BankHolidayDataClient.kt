package uk.gov.dluhc.printapi.client

import com.fasterxml.jackson.annotation.JsonProperty
import mu.KotlinLogging
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

interface BankHolidayDataClient {

    /**
     * Retrieves bank holiday dates for the requested [BankHolidayDivision] (e.g. England and Wales), starting from the
     * provided [LocalDate].
     *
     * @param division The UK nation (e.g. Scotland) that the bank holidays are required for.
     * @param fromDate The [LocalDate] from which the bank holidays start (defaults to today).
     * @param toDate The [LocalDate] when the bank holidays end (defaults to an arbitrary 100 days in the future).
     * @return a [List] of [LocalDate] values representing upcoming bank holidays.
     */
    fun getBankHolidayDates(
        division: BankHolidayDivision,
        fromDate: LocalDate = LocalDate.now(),
        toDate: LocalDate = LocalDate.now().plusDays(100)
    ): List<LocalDate>
}

/**
 * The bank holiday data we retrieve from gov.uk is structured according to the UK nation (or "division" in the JSON).
 */
enum class BankHolidayDivision(val value: String) {
    ENGLAND_AND_WALES("england-and-wales"),
    SCOTLAND("scotland"),
    NORTHERN_IRELAND("northern-ireland");

    companion object {
        @JvmStatic
        fun fromGssCode(gssCode: String): BankHolidayDivision {
            return when (gssCode.substring(0, 1)) {
                "E", "W" -> ENGLAND_AND_WALES
                "S" -> SCOTLAND
                "N" -> NORTHERN_IRELAND
                else -> {
                    logger.warn("Unknown country prefix for gssCode: [$gssCode], defaulting to $ENGLAND_AND_WALES")
                    ENGLAND_AND_WALES
                }
            }
        }
    }
}

/**
 * Data class to deserialize the JSON from gov.uk's Bank Holidays API (https://www.api.gov.uk/gds/bank-holidays).
 */
data class BankHolidayData(
    @JsonProperty("division")
    val division: String?,
    @JsonProperty("events")
    val events: List<BankHoliday>?
)

data class BankHoliday(
    @JsonProperty("name")
    val name: String?,
    @JsonProperty("date")
    val date: LocalDate?,
    @JsonProperty("notes")
    val notes: String?,
    @JsonProperty("bunting")
    val bunting: Boolean?
)
