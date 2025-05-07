package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.models.CertificateSearchBy
import uk.gov.dluhc.printapi.rest.CertificateSearchQueryStringParameters
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSearchCriteriaDto
import uk.gov.dluhc.printapi.dto.CertificateSearchBy as CertificateSearchByDto

class CertificateSearchQueryStringParametersMapperTest {

    private val mapper = CertificateSearchQueryStringParametersMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            ", ,,", // null [searchBy, searchValue]
            "APPLICATION_REFERENCE, V123ABC456, APPLICATION_REFERENCE",
        ]
    )
    fun `should map CertificateSearchQueryStringParameters to CertificateSearchCriteriaDto given searchBy and searchName values`(
        apiSearchBy: CertificateSearchBy?,
        searchValue: String?,
        expectedSearchBy: CertificateSearchByDto?
    ) {
        // Given
        val eroId = aValidEroId()
        val searchQueryStringParams = CertificateSearchQueryStringParameters(
            page = 2,
            pageSize = 10,
            searchBy = apiSearchBy,
            searchValue = searchValue
        )

        val expected = buildCertificateSearchCriteriaDto(
            eroId = eroId,
            page = 2,
            pageSize = 10,
            searchBy = expectedSearchBy,
            searchValue = searchValue
        )

        // When
        val actual = mapper.toCertificateSearchCriteriaDto(eroId, searchQueryStringParams)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
