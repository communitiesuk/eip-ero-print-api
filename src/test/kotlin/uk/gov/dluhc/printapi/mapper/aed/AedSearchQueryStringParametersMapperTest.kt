package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.models.AedSearchBy
import uk.gov.dluhc.printapi.rest.aed.AedSearchQueryStringParameters
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.dto.aed.AedSearchBy as AedSearchByDto

class AedSearchQueryStringParametersMapperTest {

    private val mapper = AedSearchQueryStringParametersMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            ", ,,", // null [searchBy, searchValue]
            "SURNAME, Thomas, SURNAME",
        ]
    )
    fun `should map AedSearchQueryStringParameters to AnonymousSearchCriteriaDto given searchBy and searchName values`(
        apiSearchBy: AedSearchBy?,
        searchValue: String?,
        expectedSearchBy: AedSearchByDto?
    ) {
        // Given
        val eroId = aValidEroId()
        val searchQueryStringParams = AedSearchQueryStringParameters(
            page = 2,
            pageSize = 10,
            searchBy = apiSearchBy,
            searchValue = searchValue
        )

        val expected = buildAnonymousSearchCriteriaDto(
            eroId = eroId,
            page = 2,
            pageSize = 10,
            searchBy = expectedSearchBy,
            searchValue = searchValue
        )

        // When
        val actual = mapper.toAnonymousSearchCriteriaDto(eroId, searchQueryStringParams)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
