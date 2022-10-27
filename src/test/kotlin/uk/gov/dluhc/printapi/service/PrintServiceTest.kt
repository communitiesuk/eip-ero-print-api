package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.mapper.PrintDetailsMapper
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage

@ExtendWith(MockitoExtension::class)
class PrintServiceTest {
    @InjectMocks
    private lateinit var printService: PrintService

    @Mock
    private lateinit var eroClient: ElectoralRegistrationOfficeManagementApiClient

    @Mock
    private lateinit var printDetailsMapper: PrintDetailsMapper

    @Mock
    private lateinit var printDetailsRepository: PrintDetailsRepository

    @Test
    fun `should save send application to print message`() {
        // Given
        val ero = buildEroManagementApiEroDto()
        val localAuthority = ero.localAuthorities[1]
        val message = buildSendApplicationToPrintMessage(gssCode = localAuthority.gssCode)
        val printDetails = buildPrintDetails()
        given(eroClient.getElectoralRegistrationOffice(any())).willReturn(ero)
        given(printDetailsMapper.toPrintDetails(any(), any(), any())).willReturn(printDetails)

        // When
        printService.savePrintMessage(message)

        // Then
        verify(eroClient).getElectoralRegistrationOffice(message.gssCode!!)
        verify(printDetailsMapper).toPrintDetails(message, ero, localAuthority.name)
        verify(printDetailsRepository).save(printDetails)
    }
}
