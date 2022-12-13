package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity

class DeliveryAddressTypeMapperTest {

    private val mapper = DeliveryAddressTypeMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "REGISTERED, REGISTERED",
            "ERO_MINUS_COLLECTION, ERO_COLLECTION",
            "ALTERNATIVE, ALTERNATIVE",
        ]
    )
    fun `should map to entity`(source: DeliveryAddressType, expected: DeliveryAddressTypeEntity) {
        // Given

        // When
        val actual = mapper.toDeliveryAddressTypeEntity(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
