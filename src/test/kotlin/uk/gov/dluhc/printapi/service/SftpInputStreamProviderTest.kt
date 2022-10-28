package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aFileDetails

@ExtendWith(MockitoExtension::class)
internal class SftpInputStreamProviderTest {
    @Mock
    private lateinit var printRequestsFileProducer: PrintRequestsFileProducer
    @Mock
    private lateinit var s3Client: S3Client
    @Mock
    private lateinit var taskExecutor: ThreadPoolTaskExecutor

    @InjectMocks
    private lateinit var sftpInputStreamProvider: SftpInputStreamProvider

    @Test
    fun `should create SFTP input stream`() {
        // Given
        val fileDetails = aFileDetails()

        // When
        val inputStream = sftpInputStreamProvider.createSftpInputStream(fileDetails)

        // Then
        verify(taskExecutor).execute(any())
        assertThat(inputStream).isNotNull
    }
}
