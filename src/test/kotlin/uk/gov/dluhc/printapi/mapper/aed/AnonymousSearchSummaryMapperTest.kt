package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.mapper.InstantMapper
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocumentSummaryEntity
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAedSearchSummaryApi
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class AnonymousSearchSummaryMapperTest {

    @Mock
    private lateinit var instantMapper: InstantMapper

    @InjectMocks
    private lateinit var mapper: AnonymousSearchSummaryMapperImpl

    @Nested
    inner class ToAnonymousSearchSummaryDto {

        @Test
        fun `should map AnonymousElectorDocumentSummary Entity to an AnonymousSearchSummaryDto`() {
            // Given
            val entity = buildAnonymousElectorDocumentSummaryEntity()
            val expected = with(entity) {
                buildAnonymousSearchSummaryDto(
                    gssCode = gssCode,
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    certificateNumber = certificateNumber,
                    electoralRollNumber = electoralRollNumber,
                    firstName = firstName,
                    surname = surname,
                    postcode = postcode,
                    issueDate = issueDate,
                    dateTimeCreated = dateCreated,
                )
            }

            // When
            val actual = mapper.toAnonymousSearchSummaryDto(entity)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verifyNoInteractions(instantMapper)
        }
    }

    @Nested
    inner class ToAnonymousSearchSummaryApi {
        @Test
        fun `should map AnonymousSearchSummaryDto to an AedSearchSummary Api`() {
            // Given
            val dto = buildAnonymousSearchSummaryDto()
            val expectedOffsetDateTime = OffsetDateTime.now()

            given(instantMapper.toOffsetDateTime(any())).willReturn(expectedOffsetDateTime)

            val expected = with(dto) {
                buildAedSearchSummaryApi(
                    gssCode = gssCode,
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    certificateNumber = certificateNumber,
                    electoralRollNumber = electoralRollNumber,
                    firstName = firstName,
                    surname = surname,
                    postcode = postcode,
                    issueDate = issueDate,
                    dateTimeCreated = expectedOffsetDateTime,
                )
            }

            // When
            val actual = mapper.toAnonymousSearchSummaryApi(dto)

            // Then
            assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
            verify(instantMapper).toOffsetDateTime(dto.dateTimeCreated)
            verifyNoMoreInteractions(instantMapper)
        }
    }
}