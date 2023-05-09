package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity
import uk.gov.dluhc.printapi.dto.DeliveryAddressType as DtoDeliveryAddressType
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType as DeliveryAddressTypeSqs
import uk.gov.dluhc.printapi.models.DeliveryAddressType as ApiDeliveryAddressType

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
    fun `should map messaging model to entity`(source: DeliveryAddressTypeSqs, expected: DeliveryAddressTypeEntity) {
        // Given

        // When
        val actual = mapper.mapSqsToEntity(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "REGISTERED, REGISTERED",
            "ERO_MINUS_COLLECTION, ERO_COLLECTION",
            "ALTERNATIVE, ALTERNATIVE",
        ]
    )
    fun `should map api model to dto`(source: ApiDeliveryAddressType, expected: DtoDeliveryAddressType) {
        // Given

        // When
        val actual = mapper.mapApiToDto(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "REGISTERED, REGISTERED",
            "ERO_COLLECTION, ERO_MINUS_COLLECTION",
            "ALTERNATIVE, ALTERNATIVE",
        ]
    )
    fun `should map dto to api`(source: DtoDeliveryAddressType, expected: ApiDeliveryAddressType) {
        // Given

        // When
        val actual = mapper.mapDtoToApi(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "REGISTERED, REGISTERED",
            "ERO_COLLECTION, ERO_COLLECTION",
            "ALTERNATIVE, ALTERNATIVE",
        ]
    )
    fun `should map dto to entity`(source: DtoDeliveryAddressType, expected: DeliveryAddressTypeEntity) {
        // Given

        // When
        val actual = mapper.mapDtoToEntity(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "REGISTERED, REGISTERED",
            "ERO_COLLECTION, ERO_COLLECTION",
            "ALTERNATIVE, ALTERNATIVE",
        ]
    )
    fun `should map entity to dto`(source: DeliveryAddressTypeEntity, expected: DtoDeliveryAddressType) {
        // Given

        // When
        val actual = mapper.mapEntityToDto(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
