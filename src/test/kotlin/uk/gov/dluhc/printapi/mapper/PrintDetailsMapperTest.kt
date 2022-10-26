package uk.gov.dluhc.printapi.mapper

import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.CertificateDelivery
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.getAMongoDbId
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.time.LocalDate
import java.util.UUID
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage as CertificateLanguageModel
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeModel

@ExtendWith(MockitoExtension::class)
class PrintDetailsMapperTest {
    @InjectMocks
    private lateinit var mapper: PrintDetailsMapperImpl

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @ParameterizedTest
    @CsvSource(value = ["EN, EN", "CY, CY"])
    fun `should map send application to print message to print details`(
        certificateLanguageModel: CertificateLanguageModel,
        certificateLanguageEntity: CertificateLanguageEntity
    ) {
        // Given
        val ero = buildEroManagementApiEroDto()
        val localAuthority = ero.localAuthorities[0]
        val message = buildSendApplicationToPrintMessage(certificateLanguage = certificateLanguageModel)
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(SourceTypeEntity.VOTER_CARD)

        val expected = with(message) {
            PrintDetails(
                id = UUID.randomUUID(),
                requestId = getAMongoDbId(),
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                sourceType = SourceTypeEntity.VOTER_CARD,
                vacNumber = randomAlphanumeric(20),
                requestDateTime = requestDateTime,
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = certificateLanguageEntity,
                photoLocation = photoLocation,
                delivery = with(delivery) {
                    CertificateDelivery(
                        addressee = addressee,
                        address = with(address) {
                            Address(
                                street = street,
                                postcode = postcode,
                                property = property,
                                locality = locality,
                                town = town,
                                area = area,
                                uprn = uprn
                            )
                        },
                        deliveryClass = DeliveryClass.STANDARD
                    )
                },
                gssCode = gssCode,
                issuingAuthority = localAuthority.name,
                issueDate = LocalDate.now(),
                eroEnglish = with(ero) {
                    ElectoralRegistrationOffice(
                        name = name,
                        phoneNumber = null,
                        emailAddress = null,
                        website = null,
                        address = null
                    )
                },
                eroWelsh = if (certificateLanguageModel == CertificateLanguage.EN) null else with(ero) {
                    ElectoralRegistrationOffice(
                        name = name,
                        phoneNumber = null,
                        emailAddress = null,
                        website = null,
                        address = null
                    )
                }
            )
        }
        // When

        val actual = mapper.toPrintDetails(message, ero, localAuthority.name)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringFields("id", "requestId", "vacNumber").isEqualTo(expected)
        assertThat(actual.id).isNotNull
        assertThat(actual.requestId).isNotNull.containsPattern(Regex("^[a-f\\d]{24}$").toPattern())
        assertThat(actual.vacNumber).isNotNull.containsPattern(Regex("^[A-Za-z\\d]{20}$").toPattern())
        verify(sourceTypeMapper).toSourceTypeEntity(SourceTypeModel.VOTER_MINUS_CARD)
    }

    @Test
    fun `should map id requestId and vacNumber to random values`() {
        // Given
        val ero = buildEroManagementApiEroDto()
        val localAuthority = ero.localAuthorities[0]
        val message = buildSendApplicationToPrintMessage()
        given(sourceTypeMapper.toSourceTypeEntity(any())).willReturn(SourceTypeEntity.VOTER_CARD)
        val results = mutableListOf<PrintDetails>()

        // When

        repeat(100) { results.add(mapper.toPrintDetails(message, ero, localAuthority.name)) }

        // Then
        assertThat(results).extracting("id").doesNotHaveDuplicates()
        assertThat(results).extracting("requestId").doesNotHaveDuplicates()
        assertThat(results).extracting("vacNumber").doesNotHaveDuplicates()
    }
}
