package uk.gov.dluhc.printapi.messaging

import com.jcraft.jsch.ChannelSftp
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.integration.file.remote.InputStreamCallback
import org.springframework.test.context.transaction.TestTransaction
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration.Companion.S3_BUCKET_CONTAINING_PHOTOS
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_REQUEST_UPLOAD_PATH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.entity.Delivery
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildProcessPrintRequestBatchMessage
import java.io.ByteArrayInputStream
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.transaction.Transactional

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProcessPrintRequestBatchMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    @Transactional
    fun `should process print request batch message`() {
        // Given
        val printDetailsId = UUID.randomUUID()
        val batchId = aValidBatchId()
        val requestId = aValidRequestId()
        val s3ResourceContents = "S3 Object Contents"
        val s3Bucket = S3_BUCKET_CONTAINING_PHOTOS
        val s3Path =
            "E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"

        // add resource to S3
        val s3Resource = s3ResourceContents.encodeToByteArray()
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Path)
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )

        // save print requests in DynamoDB
        val details = buildPrintDetails(
            id = printDetailsId,
            batchId = batchId,
            printRequestStatuses = mutableListOf(
                PrintRequestStatus(ASSIGNED_TO_BATCH, OffsetDateTime.now(clock), OffsetDateTime.now(clock))
            ),
            requestId = requestId,
            eroWelsh = buildElectoralRegistrationOffice(),
            photoLocation = "arn:aws:s3:::$s3Bucket/$s3Path"
        )
        printDetailsRepository.save(details)

        // save certificates in MySQL
        var certificate = Certificate(
            vacNumber = details.vacNumber,
            sourceType = details.sourceType,
            sourceReference = details.sourceReference,
            applicationReference = details.applicationReference,
            applicationReceivedDateTime = details.applicationReceivedDateTime?.toInstant(),
            issuingAuthority = details.issuingAuthority,
            issueDate = details.issueDate,
            suggestedExpiryDate = details.suggestedExpiryDate,
            gssCode = details.gssCode,
            status = details.printRequestStatuses!![0].status,
            printRequests = mutableListOf(
                PrintRequest(
                    requestId = details.requestId,
                    vacVersion = details.vacVersion,
                    requestDateTime = details.requestDateTime!!.toInstant(),
                    firstName = details.firstName,
                    middleNames = details.middleNames,
                    surname = details.surname,
                    certificateLanguage = details.certificateLanguage,
                    certificateFormat = details.certificateFormat,
                    photoLocationArn = details.photoLocation,
                    delivery = Delivery(
                        address = Address(
                            street = details.delivery?.address?.street,
                            postcode = details.delivery?.address?.postcode,
                            property = details.delivery?.address?.property,
                            locality = details.delivery?.address?.locality,
                            town = details.delivery?.address?.town,
                            area = details.delivery?.address?.area,
                            uprn = details.delivery?.address?.uprn,
                        ),
                        addressee = details.delivery?.addressee,
                        deliveryClass = details.delivery?.deliveryClass,
                        deliveryMethod = details.delivery?.deliveryMethod
                    ),
                    eroEnglish = ElectoralRegistrationOffice(
                        name = details.eroEnglish?.name,
                        phoneNumber = details.eroEnglish?.phoneNumber,
                        emailAddress = details.eroEnglish?.emailAddress,
                        website = details.eroEnglish?.website,
                        address = Address(
                            street = details.eroEnglish?.address?.street,
                            postcode = details.eroEnglish?.address?.postcode,
                            property = details.eroEnglish?.address?.property,
                            locality = details.eroEnglish?.address?.locality,
                            town = details.eroEnglish?.address?.town,
                            area = details.eroEnglish?.address?.area,
                            uprn = details.eroEnglish?.address?.uprn,
                        )
                    ),
                    eroWelsh = ElectoralRegistrationOffice(
                        name = details.eroWelsh?.name,
                        phoneNumber = details.eroWelsh?.phoneNumber,
                        emailAddress = details.eroWelsh?.emailAddress,
                        website = details.eroWelsh?.website,
                        address = Address(
                            street = details.eroWelsh?.address?.street,
                            postcode = details.eroWelsh?.address?.postcode,
                            property = details.eroWelsh?.address?.property,
                            locality = details.eroWelsh?.address?.locality,
                            town = details.eroWelsh?.address?.town,
                            area = details.eroWelsh?.address?.area,
                            uprn = details.eroWelsh?.address?.uprn,
                        )
                    ),
                    batchId = details.batchId,
                    userId = details.userId,
                    statusHistory = mutableListOf(
                        uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus(
                            status = details.printRequestStatuses!![0].status,
                            eventDateTime = details.printRequestStatuses!![0].eventDateTime?.toInstant(),
                            message = details.printRequestStatuses!![0].message
                        )
                    )
                )
            )
        )
        certificate = certificateRepository.save(certificate)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThat(filterListForName(batchId)).isEmpty()

        // add message to queue for processing
        val payload = buildProcessPrintRequestBatchMessage(batchId = batchId)

        // When
        TestTransaction.start()
        sqsMessagingTemplate.convertAndSend(processPrintRequestBatchQueueName, payload)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // Then
        TestTransaction.start()
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val sftpDirectoryList = filterListForName(batchId)
            assertThat(sftpDirectoryList).hasSize(1)
            verifySftpZipFile(sftpDirectoryList, batchId, requestId, s3ResourceContents)
            val processedPrintDetails = printDetailsRepository.get(printDetailsId)
            assertThat(processedPrintDetails.status).isEqualTo(Status.SENT_TO_PRINT_PROVIDER)
            val processedCertificate = certificateRepository.findById(certificate.id!!).get()
            assertThat(processedCertificate.status).isEqualTo(Status.SENT_TO_PRINT_PROVIDER)
        }
    }

    private fun verifySftpZipFile(
        sftpDirectoryList: List<ChannelSftp.LsEntry>,
        batchId: String,
        requestId: String,
        s3ResourceContents: String
    ) {
        val filename = sftpDirectoryList[0].filename
        assertThat(filename).matches("$batchId-\\d{17}-1.zip")
        val expectedPhotoPathInZip = "$batchId-$requestId.png"
        sftpInboundTemplate.get(
            "$PRINT_REQUEST_UPLOAD_PATH/$filename",
            (
                InputStreamCallback { stream ->
                    run {
                        val zipFile = ZipInputStream(stream)
                        val psvFile = zipFile.nextEntry
                        assertThat(psvFile).isNotNull
                        assertThat(psvFile!!.name).matches("$batchId-\\d{17}-1.psv")
                        val psvContents = String(zipFile.readBytes())
                        assertThat(psvContents).contains(expectedPhotoPathInZip)
                        val photoFile = zipFile.nextEntry
                        assertThat(photoFile).isNotNull
                        assertThat(photoFile!!.name).isEqualTo(expectedPhotoPathInZip)
                        val photoFileContents = String(zipFile.readBytes())
                        assertThat(photoFileContents).isEqualTo(s3ResourceContents)
                    }
                }
                )
        )
    }

    private fun filterListForName(batchId: String) =
        sftpInboundTemplate.list(PRINT_REQUEST_UPLOAD_PATH).filter { lsEntry -> lsEntry.filename.contains(batchId) }
}
