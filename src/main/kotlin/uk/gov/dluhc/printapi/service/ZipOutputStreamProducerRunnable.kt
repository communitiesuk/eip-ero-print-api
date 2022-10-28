package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import software.amazon.awssdk.core.sync.ResponseTransformer
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val logger = KotlinLogging.logger { }

/**
 * Runnable that generates a ZIP output stream that sources data from
 * the ZIP print requests file writer and photos from S3.
 */
class ZipOutputStreamProducerRunnable(
    private val s3Client: S3Client,
    private val zipOutputStream: OutputStream,
    private val sftpInputStream: InputStream,
    private val fileDetails: FileDetails,
    private val printRequestsFileProducer: PrintRequestsFileProducer
) : Runnable {

    override fun run() {
        ZipOutputStream(zipOutputStream).use { out ->
            try {
                addPrintRequestFile(out)
                addPhotos(out)
            } catch (ex: Exception) {
                // close the pipe so error is propagated
                logger.error("Exception producing Printer Zip Output Stream", ex)
                sftpInputStream.close()
            }
        }
    }

    private fun addPrintRequestFile(zipStream: ZipOutputStream) {
        val zipEntry = ZipEntry(fileDetails.printRequestsFilename)
        zipStream.putNextEntry(zipEntry)
        printRequestsFileProducer.writeFileToStream(zipStream, fileDetails.printRequests)
        zipStream.closeEntry()
    }

    private fun addPhotos(out: ZipOutputStream) {
        fileDetails.photoLocations.forEach { photo -> addPhoto(out, photo) }
    }

    private fun addPhoto(zipStream: ZipOutputStream, photoLocation: PhotoLocation) {
        with(photoLocation) {
            val zipEntry = ZipEntry(zipPath)
            zipStream.putNextEntry(zipEntry)
            // object is read directly to the zip stream
            s3Client.getObject(
                getObjectRequest(),
                ResponseTransformer.toOutputStream(zipStream)
            )
            zipStream.closeEntry()
        }
    }

    private fun PhotoLocation.getObjectRequest(): GetObjectRequest =
        GetObjectRequest.builder().bucket(sourceBucket).key(sourcePath).build()
}
