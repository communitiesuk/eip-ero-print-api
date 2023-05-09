package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.dluhc.printapi.models.AedSearchSummaryResponse
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus.PRINTED
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchSummaryResults
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocumentSummaryEntity
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAedSearchSummaryApi
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAedSearchSummaryApiFromAnonymousSearchSummaryDto
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class AnonymousSearchSummaryMapperTest {

    @Mock
    private lateinit var instantMapper: InstantMapper

    @InjectMocks
    private lateinit var mapper: AnonymousSearchSummaryMapperImpl

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
                status = PRINTED,
                firstName = firstName,
                surname = surname,
                postcode = postcode,
                issueDate = issueDate,
                dateTimeCreated = expectedOffsetDateTime,
            )
        }

        // When
        val actual = mapper.toAedSearchSummaryApi(dto)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(instantMapper).toOffsetDateTime(dto.dateTimeCreated)
        verifyNoMoreInteractions(instantMapper)
    }

    @Test
    fun `should map AnonymousSearchSummaryResults Dto to an AedSearchSummaryResponse`() {
        // Given
        val aedSearchSummaryDto = buildAnonymousSearchSummaryDto()
        val aedSummaryResults = buildAnonymousSearchSummaryResults(results = listOf(aedSearchSummaryDto))
        val expectedOffsetDateTime = OffsetDateTime.now()

        given(instantMapper.toOffsetDateTime(any())).willReturn(expectedOffsetDateTime)

        val expected = with(aedSummaryResults) {
            AedSearchSummaryResponse(
                page = page,
                pageSize = pageSize,
                totalPages = totalPages,
                totalResults = totalResults,
                results = listOf(
                    buildAedSearchSummaryApiFromAnonymousSearchSummaryDto(
                        dto = aedSearchSummaryDto,
                        dateTimeCreated = expectedOffsetDateTime
                    )
                ),
            )
        }

        // When
        val actual = mapper.toAedSearchSummaryResponse(aedSummaryResults)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(instantMapper).toOffsetDateTime(aedSearchSummaryDto.dateTimeCreated)
        verifyNoMoreInteractions(instantMapper)
    }
}
