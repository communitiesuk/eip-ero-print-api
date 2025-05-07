package uk.gov.dluhc.printapi.service

import org.apache.sshd.sftp.client.SftpClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidInputStream
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintResponseFileName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSftpPath
import uk.gov.dluhc.printapi.testsupport.testdata.aValidZipFilename

@ExtendWith(MockitoExtension::class)
internal class SftpServiceTest {

    @Mock
    private lateinit var sftpInboundTemplate: SftpRemoteFileTemplate

    @Mock
    private lateinit var sftpOutboundTemplate: SftpRemoteFileTemplate

    // @InjectMocks doesn't seem to be able to handle injecting 2 params of the same type
    // so initialising in setUp method below
    private lateinit var sftpService: SftpService

    @BeforeEach
    fun setUp() {
        sftpService = SftpService(sftpInboundTemplate, sftpOutboundTemplate)
    }

    @Test
    fun `should send file to SFTP`() {
        // Given
        val inputStream = aValidInputStream()
        val zipFilename = aValidZipFilename()
        val path = aValidSftpPath()
        given(sftpInboundTemplate.send(any())).willReturn(path)

        // When
        val sftpPath = sftpService.sendFile(inputStream, zipFilename)

        // Then
        verify(sftpInboundTemplate).send(argThat { msg -> msg.payload == inputStream && msg.headers.containsValue(zipFilename) })
        assertThat(sftpPath).isEqualTo(path)
        verifyNoInteractions(sftpOutboundTemplate)
    }

    @Nested
    inner class IdentifyFilesToBeProcessed {

        @Test
        fun `should return empty list on SFTP folder when no files are present`() {
            // Given
            val filesDirectoryPath = "/sftp/OutBound"
            given(sftpOutboundTemplate.list(any())).willReturn(emptyList<SftpClient.DirEntry>().toTypedArray())

            // When
            val fileList = sftpService.identifyFilesToBeProcessed(filesDirectoryPath)

            // Then
            assertThat(fileList).isEmpty()
            verify(sftpOutboundTemplate).list(filesDirectoryPath)
            verifyNoInteractions(sftpInboundTemplate)
        }

        @Test
        fun `should return empty list on SFTP folder when only processing files are present`() {
            // Given
            val dirEntry1 = mockDirEntryForFilename("status-20221101171156056.json.processing")
            val dirEntry2 = mockDirEntryForFilename("status-20221125171156053.json.processing")
            val dirEntry3 = mockDirEntryForFilename("status-20221031171156051.json.processing")
            val filesDirectoryPath = "/sftp/OutBound"

            given(sftpOutboundTemplate.list(any())).willReturn(listOf(dirEntry1, dirEntry2, dirEntry3).toTypedArray())

            // When
            val fileList = sftpService.identifyFilesToBeProcessed(filesDirectoryPath)

            // Then
            assertThat(fileList).isEmpty()
            verify(sftpOutboundTemplate).list(filesDirectoryPath)
            verifyNoInteractions(sftpInboundTemplate)
        }

        @Test
        fun `should list files on SFTP folder`() {
            // Given
            val matchedFile1 = mockDirEntryForFilename(aValidPrintResponseFileName())
            val matchedFile2 = mockDirEntryForFilename("status-20221101171156056.json")
            val matchedFile3 = mockDirEntryForFilename("status-20221125171156053.json")
            val matchedFile4 = mockDirEntryForFilename("status-20221031171156051.json")
            val alreadyProcessedFile = mockDirEntryForFilename("status-20221201671156099.json.processing")
            val expectedFileCount = 4
            val filesDirectoryPath = "/sftp/OutBound"

            given(sftpOutboundTemplate.list(any()))
                .willReturn(
                    listOf(
                        matchedFile1, matchedFile2, matchedFile3, matchedFile4, alreadyProcessedFile,
                    ).toTypedArray()
                )

            // When
            val fileList = sftpService.identifyFilesToBeProcessed(filesDirectoryPath)

            // Then
            assertThat(fileList)
                .hasSize(expectedFileCount)
                .containsExactlyInAnyOrder(
                    matchedFile1.filename,
                    matchedFile2.filename,
                    matchedFile3.filename,
                    matchedFile4.filename
                )
                .doesNotContain(alreadyProcessedFile.filename)
            verify(sftpOutboundTemplate).list(filesDirectoryPath)
            verifyNoInteractions(sftpInboundTemplate)
        }

        @ParameterizedTest
        @CsvSource(
            value = [
                "sftp/OutBound/status-20221101171156056.json",
                "status-ABCDEFGHIJKLMNOPQ.json",
                "status-20221201671156099.json.processing",
                "status-20221105171156065.json.json",
                "status-20221201671156065.processing.json",
                "status-20221201671150.json",
                "STATUS-20221101171156078.json",
                "STATUS-20221101171156078.json.doc"
            ]
        )
        fun `should not list files on SFTP folder for filename not matching pattern`(fileName: String) {
            // Given
            val mismatchedPatternFile = mockDirEntryForFilename(fileName)
            val filesDirectoryPath = "/sftp/OutBound"

            given(sftpOutboundTemplate.list(any())).willReturn(listOf(mismatchedPatternFile).toTypedArray())

            // When
            val fileList = sftpService.identifyFilesToBeProcessed(filesDirectoryPath)

            // Then
            assertThat(fileList).isEmpty()
            verify(sftpOutboundTemplate).list(filesDirectoryPath)
            verifyNoInteractions(sftpInboundTemplate)
        }
    }

    @Nested
    inner class MarkFileForProcessing {

        @Test
        fun `should rename files on SFTP folder`() {
            // Given
            val fileDirectoryPath = "/sftp/OutBound"
            val fileName = "file.json"

            // When
            val renamedFile = sftpService.markFileForProcessing(fileDirectoryPath, fileName)

            // Then
            assertThat(renamedFile).isEqualTo("file.json.processing")
            verify(sftpOutboundTemplate).rename("/sftp/OutBound/file.json", "/sftp/OutBound/file.json.processing")
            verifyNoInteractions(sftpInboundTemplate)
        }
    }

    private fun mockDirEntryForFilename(filename: String): SftpClient.DirEntry {
        val dirEntry: SftpClient.DirEntry = mock(SftpClient.DirEntry::class.java)
        given(dirEntry.filename).willReturn(filename)
        return dirEntry
    }
}
