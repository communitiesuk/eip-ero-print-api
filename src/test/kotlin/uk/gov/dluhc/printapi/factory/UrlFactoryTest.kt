package uk.gov.dluhc.printapi.factory

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.dto.SourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference

class UrlFactoryTest {

    private val factory = UrlFactory("http://localhost:8080")

    @Test
    fun `should create photo URL given source type ANONYMOUS_ELECTOR_DOCUMENT`() {
        // Given
        val eroId = aValidEroId()
        val sourceReference = aValidSourceReference()
        val sourceType = SourceType.ANONYMOUS_ELECTOR_DOCUMENT

        // When
        val actual = factory.createPhotoUrl(eroId, sourceType, sourceReference)

        // Then
        assertThat(actual).isEqualTo("http://localhost:8080/eros/$eroId/anonymous-elector-documents/photo?applicationId=$sourceReference")
    }

    @Test
    fun `should create photo URL given source type VOTER_CARD`() {
        // Given
        val eroId = aValidEroId()
        val sourceReference = aValidSourceReference()
        val sourceType = SourceType.VOTER_CARD

        // When
        val actual = factory.createPhotoUrl(eroId, sourceType, sourceReference)

        // Then
        assertThat(actual).isEqualTo("http://localhost:8080/eros/$eroId/certificates/photo?applicationId=$sourceReference")
    }
}
