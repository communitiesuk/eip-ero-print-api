package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.dto.aed.UpdateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildUpdateAnonymousElectorDocumentRequest

class UpdateAnonymousElectorDocumentMapperTest {
    private val mapper = UpdateAnonymousElectorDocumentMapperImpl()

    @Test
    fun `should map to UpdateAnonymousElectorDocumentDto`() {
        // Given
        val request = buildUpdateAnonymousElectorDocumentRequest()
        val expected = UpdateAnonymousElectorDocumentDto(request.sourceReference, request.email, request.phoneNumber)

        // When
        val actual = mapper.toUpdateAedDto(request)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
