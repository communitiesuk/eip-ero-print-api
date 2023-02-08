package uk.gov.dluhc.printapi.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.MapType
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import java.time.LocalDate
import javax.annotation.PostConstruct

@Component
class BankHolidayDataFileClient(
    @Value("classpath:bank-holidays.json") private val resource: Resource,
    private val mapper: ObjectMapper
) : BankHolidayDataClient {

    private var bankHolidayData: Map<String, BankHolidayData> = emptyMap()

    @PostConstruct
    fun loadBankHolidayData() {
        val mapType: MapType = mapType()
        bankHolidayData = mapper.readValue(resource.inputStream, mapType)
    }

    override fun getBankHolidayDates(
        division: BankHolidayDivision,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<LocalDate> {
        val bankHolidays = bankHolidayData[division.value]?.events ?: emptyList()
        return bankHolidays
            .filter { upcomingEvents(it, fromDate, toDate) }
            .map { e -> e.date!! }
    }

    private fun mapType(): MapType {
        return mapper.typeFactory.constructMapType(
            HashMap::class.java,
            String::class.java,
            BankHolidayData::class.java
        )
    }

    private fun upcomingEvents(
        bankHoliday: BankHoliday,
        fromDate: LocalDate,
        toDate: LocalDate
    ) = bankHoliday.date!!.isAfter(fromDate) && bankHoliday.date.isBefore(toDate)
}
