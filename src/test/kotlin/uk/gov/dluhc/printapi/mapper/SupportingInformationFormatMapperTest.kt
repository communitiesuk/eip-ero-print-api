package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntityEnum
import uk.gov.dluhc.printapi.dto.SupportingInformationFormat as SupportingInformationFormatDto
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat as SupportingInformationFormatModelEnum
import uk.gov.dluhc.printapi.models.SupportingInformationFormat as SupportingInformationFormatApi

class SupportingInformationFormatMapperTest {

    private val mapper = SupportingInformationFormatMapperImpl()

    @Nested
    inner class ToPrintRequestApiEnum {
        @ParameterizedTest
        @CsvSource(
            value = [
                "STANDARD, STANDARD",
                "BRAILLE, BRAILLE",
                "EASY_READ, EASY_READ",
                "LARGE_PRINT, LARGE_PRINT",
            ]
        )
        fun `should map SupportingInformationFormat to print request api enum`(
            source: SupportingInformationFormatEntityEnum,
            expected: PrintRequest.CertificateFormat
        ) {
            // Given

            // When
            val actual = mapper.toPrintRequestApiEnum(source)

            // Then
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Nested
    inner class ToPrintRequestEntityEnum {
        @ParameterizedTest
        @CsvSource(
            value = [
                "STANDARD, STANDARD",
                "BRAILLE, BRAILLE",
                "LARGE_MINUS_PRINT, LARGE_PRINT",
                "EASY_MINUS_READ, EASY_READ",
            ]
        )
        fun `should map SupportingInformationFormat model enum to entity enum`(
            supportingInformationFormatModelEnum: SupportingInformationFormatModelEnum,
            expected: SupportingInformationFormatEntityEnum
        ) {
            // Given
            // When
            val actual = mapper.toPrintRequestEntityEnum(supportingInformationFormatModelEnum)

            // Then
            assertThat(actual).isEqualTo(expected)
        }
    }

    @ParameterizedTest
    @CsvSource(value = ["STANDARD, STANDARD"])
    fun `should map SupportingInformationFormat DTO enum to entity enum`(
        supportingInformationFormatModelEnum: SupportingInformationFormatDto,
        expected: SupportingInformationFormatEntityEnum
    ) {
        // Given
        // When
        val actual = mapper.mapDtoToEntity(supportingInformationFormatModelEnum)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(value = ["STANDARD, STANDARD"])
    fun `should map SupportingInformationFormat entity enum to DTO enum`(
        supportingInformationFormatModelEnum: SupportingInformationFormatEntityEnum,
        expected: SupportingInformationFormatDto
    ) {
        // Given
        // When
        val actual = mapper.mapEntityToDto(supportingInformationFormatModelEnum)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(
        value = SupportingInformationFormatEntityEnum::class,
        names = ["STANDARD"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `should throw exception for unsupported entity enum to DTO enum`(
        value: SupportingInformationFormatEntityEnum
    ) {
        // Given
        // When
        val ex = Assertions.catchThrowableOfType(
            { mapper.mapEntityToDto(value) },
            IllegalArgumentException::class.java
        )

        // Then
        assertThat(ex).isNotNull
        assertThat(ex.message).isEqualTo("Unexpected enum constant: $value")
    }

    @ParameterizedTest
    @CsvSource(value = ["STANDARD, STANDARD"])
    fun `should map SupportingInformationFormat REST API enum to DTO enum`(
        supportingInformationFormat: SupportingInformationFormatApi,
        expected: SupportingInformationFormatDto
    ) {
        // Given
        // When
        val actual = mapper.mapApiToDto(supportingInformationFormat)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(value = ["STANDARD, STANDARD"])
    fun `should map SupportingInformationFormat DTO enum to REST API enum`(
        supportingInformationFormat: SupportingInformationFormatDto,
        expected: SupportingInformationFormatApi
    ) {
        // Given
        // When
        val actual = mapper.mapDtoToApi(supportingInformationFormat)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
