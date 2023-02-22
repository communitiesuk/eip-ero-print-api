package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Spy
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.dluhc.printapi.database.entity.AedPrintRequest
import uk.gov.dluhc.printapi.database.entity.AedPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateAnonymousElectorDocumentDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class AedPrintRequestMapperTest {

    companion object {
        private const val FIXED_DATE_STRING = "2022-10-18"
        private val FIXED_DATE = LocalDate.parse(FIXED_DATE_STRING)
        private val FIXED_TIME = Instant.parse("${FIXED_DATE_STRING}T11:22:32.123Z")
        private val FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
    }

    @InjectMocks
    private lateinit var mapper: AedPrintRequestMapperImpl

    @Spy
    private val clock: Clock = FIXED_CLOCK

    @Test
    fun `should map AED generate request to AED print request`() {
        // Given
        val request = buildGenerateAnonymousElectorDocumentDto()
        val aedTemplateFilename = aValidAnonymousElectorDocumentTemplateFilename()

        val expected = with(request) {
            AedPrintRequest(
                aedTemplateFilename = aedTemplateFilename,
                electoralRollNumber = electoralRollNumber,
                requestDateTime = FIXED_TIME,
                issueDate = FIXED_DATE,
                statusHistory = mutableListOf(
                    AedPrintRequestStatus(
                        status = AedPrintRequestStatus.Status.GENERATED,
                        eventDateTime = FIXED_TIME
                    )
                ),
                userId = userId
            )
        }

        // When
        val actual = mapper.toPrintRequest(request, aedTemplateFilename)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
