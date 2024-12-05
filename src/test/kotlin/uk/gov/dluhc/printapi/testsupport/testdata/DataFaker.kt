package uk.gov.dluhc.printapi.testsupport.testdata

import net.datafaker.Faker
import org.junit.jupiter.params.provider.Arguments
import java.util.Locale

class DataFaker {
    companion object {
        val faker: Faker = Faker(Locale.UK)
    }
}

class ApplicationsApiTestSource {
    companion object {
        @JvmStatic
        fun isFromApplicationsApiTestSource(): List<Arguments> {
            return listOf(
                Arguments.of(false),
                Arguments.of(true)
            )
        }
    }
}
