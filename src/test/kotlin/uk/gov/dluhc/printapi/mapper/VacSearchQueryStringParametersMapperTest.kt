package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.models.VacSearchBy
import uk.gov.dluhc.printapi.rest.VacSearchQueryStringParameters
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacSearchCriteriaDto
import uk.gov.dluhc.printapi.dto.VacSearchBy as VacSearchByDto

class VacSearchQueryStringParametersMapperTest {

    private val mapper = VacSearchQueryStringParametersMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            ", ,,", // null [searchBy, searchValue]
            "APPLICATION_REFERENCE, V123ABC456, APPLICATION_REFERENCE",
        ]
    )
    fun `should map VacSearchQueryStringParameters to VacSearchCriteriaDto given searchBy and searchName values`(
        apiSearchBy: VacSearchBy?,
        searchValue: String?,
        expectedSearchBy: VacSearchByDto?
    ) {
        // Given
        val eroId = aValidEroId()
        val searchQueryStringParams = VacSearchQueryStringParameters(
            page = 2,
            pageSize = 10,
            searchBy = apiSearchBy,
            searchValue = searchValue
        )

        val expected = buildVacSearchCriteriaDto(
            eroId = eroId,
            page = 2,
            pageSize = 10,
            searchBy = expectedSearchBy,
            searchValue = searchValue
        )

        // When
        val actual = mapper.toVacSearchCriteriaDto(eroId, searchQueryStringParams)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
