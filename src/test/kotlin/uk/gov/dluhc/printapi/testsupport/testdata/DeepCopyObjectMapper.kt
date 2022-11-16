package uk.gov.dluhc.printapi.testsupport.testdata

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

class DeepCopyObjectMapper {
    companion object {
        val objectMapper: ObjectMapper =
            ObjectMapper().registerModule(JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false
                )
    }
}