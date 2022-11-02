package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidInputStream
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSftpPath
import uk.gov.dluhc.printapi.testsupport.testdata.aValidZipFilename

@ExtendWith(MockitoExtension::class)
internal class SftpServiceTest {

    @Mock
    private lateinit var sftpTemplate: SftpRemoteFileTemplate

    @InjectMocks
    private lateinit var sftpService: SftpService

    @Test
    fun `should send file to SFTP`() {
        // Given
        val inputStream = aValidInputStream()
        val zipFilename = aValidZipFilename()
        val path = aValidSftpPath()
        given(sftpTemplate.send(any())).willReturn(path)

        // When
        val sftpPath = sftpService.sendFile(inputStream, zipFilename)

        // Then
        verify(sftpTemplate).send(argThat { msg -> msg.payload == inputStream && msg.headers.containsValue(zipFilename) })
        assertThat(sftpPath).isEqualTo(path)
    }
}
