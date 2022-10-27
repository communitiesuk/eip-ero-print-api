package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeModel

class SourceTypeMapperTest {
    private val mapper = SourceTypeMapperImpl()

    @ParameterizedTest
    @CsvSource(value = ["VOTER_MINUS_CARD, VOTER_CARD"])
    fun `should map source type`(sourceTypeModel: SourceTypeModel, sourceTypeEntity: SourceTypeEntity) {
        // Given
        // When
        val actual = mapper.toSourceTypeEntity(sourceTypeModel)

        // Then
        assertThat(actual).isEqualTo(sourceTypeEntity)
    }
}
