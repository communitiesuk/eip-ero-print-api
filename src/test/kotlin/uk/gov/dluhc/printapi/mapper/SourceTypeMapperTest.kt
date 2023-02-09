package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.dto.SourceType as SourceTypeDto
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeSqsModel
import uk.gov.dluhc.printapi.models.SourceType as SourceTypeApi

class SourceTypeMapperTest {
    private val mapper = SourceTypeMapperImpl()

    @ParameterizedTest
    @CsvSource(value = ["VOTER_MINUS_CARD, VOTER_CARD"])
    fun `should map sqs model source type to entity`(sourceTypeModel: SourceTypeSqsModel, sourceTypeEntity: SourceTypeEntity) {
        // Given
        // When
        val actual = mapper.mapSqsToEntity(sourceTypeModel)

        // Then
        assertThat(actual).isEqualTo(sourceTypeEntity)
    }

    @ParameterizedTest
    @CsvSource(value = ["VOTER_MINUS_CARD, VOTER_CARD"])
    fun `should map api source type to dto`(sourceTypeApi: SourceTypeApi, sourceTypeDto: SourceTypeDto) {
        // Given
        // When
        val actual = mapper.mapApiToDto(sourceTypeApi)

        // Then
        assertThat(actual).isEqualTo(sourceTypeDto)
    }

    @ParameterizedTest
    @CsvSource(value = ["VOTER_CARD, VOTER_CARD"])
    fun `should map dto source type to entity`(sourceTypeDto: SourceTypeDto, sourceTypeEntity: SourceTypeEntity) {
        // Given
        // When
        val actual = mapper.mapDtoToEntity(sourceTypeDto)

        // Then
        assertThat(actual).isEqualTo(sourceTypeEntity)
    }
}
