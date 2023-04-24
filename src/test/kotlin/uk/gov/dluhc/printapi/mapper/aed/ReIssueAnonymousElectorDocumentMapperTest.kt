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
import uk.gov.dluhc.printapi.dto.DeliveryAddressType.ERO_COLLECTION
import uk.gov.dluhc.printapi.dto.aed.ReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.models.DeliveryAddressType.ERO_MINUS_COLLECTION
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildReIssueAnonymousElectorDocumentRequest

@ExtendWith(MockitoExtension::class)
class ReIssueAnonymousElectorDocumentMapperTest {

    @InjectMocks
    private lateinit var mapper: ReIssueAnonymousElectorDocumentMapperImpl

    @Mock
    private lateinit var deliveryAddressTypeMapper: DeliveryAddressTypeMapper

    @Nested
    inner class ToReIssueAnonymousElectorDocumentDto {

        @Test
        fun `should map to ReIssueAnonymousElectorDocumentDto DTO given API Request`() {
            // Given
            val userId = aValidUserId()
            val sourceReference = aValidSourceReference()
            val electoralRollNumber = aValidElectoralRollNumber()
            val deliveryAddressType = ERO_MINUS_COLLECTION

            val apiRequest = buildReIssueAnonymousElectorDocumentRequest(
                sourceReference = sourceReference,
                electoralRollNumber = electoralRollNumber,
                deliveryAddressType = deliveryAddressType
            )

            given(deliveryAddressTypeMapper.mapApiToDto(any())).willReturn(ERO_COLLECTION)

            val expected = ReIssueAnonymousElectorDocumentDto(
                userId = userId,
                sourceReference = sourceReference,
                electoralRollNumber = electoralRollNumber,
                deliveryAddressType = ERO_COLLECTION
            )

            // When
            val actual = mapper.toReIssueAnonymousElectorDocumentDto(apiRequest, userId)

            // Then
            assertThat(actual).isEqualTo(expected)
            verify(deliveryAddressTypeMapper).mapApiToDto(ERO_MINUS_COLLECTION)
        }
    }
}
