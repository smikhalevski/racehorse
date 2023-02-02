package org.racehorse.evergreen

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.racehorse.evergreen.UpdateWorker.Companion.TARGET_DIR
import org.racehorse.evergreen.UpdateWorker.Companion.URL
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_PARTIAL
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Downloads ZIP file from [URL] and unpacks it to the [TARGET_DIR].
 *
 * The presence of [TARGET_DIR] means that download succeeded.
 */
open class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        /**
         * The URL of the ZIP file.
         */
        const val URL = "url"

        /**
         * The directory to which the ZIP file contents are written.
         */
        const val TARGET_DIR = "targetDir"

        /**
         * The read buffer size in bytes. Default is 8192.
         */
        const val BUFFER_SIZE = "bufferSize"

        /**
         * The content length of the ZIP file in bytes (int), or -1 if unknown.
         */
        const val CONTENT_LENGTH = "contentLength"

        /**
         * The read length in bytes (long).
         */
        const val READ_LENGTH = "readLength"
    }

    /**
     * Creates a URL connection. Override this method to add header or authentication if needed.
     */
    open fun openConnection(url: String): HttpURLConnection {
        return URL(url).openConnection() as HttpURLConnection
    }

    private lateinit var targetDir: File
    private lateinit var unpackDir: File
    private lateinit var zipFile: File
    private lateinit var etagFile: File

    override suspend fun doWork(): Result {
        val url = requireNotNull(inputData.getString(URL))
        val targetDirPath = requireNotNull(inputData.getString(TARGET_DIR))
        val bufferSize = inputData.getInt(BUFFER_SIZE, 8192)

        targetDir = File(targetDirPath)
        unpackDir = File("$targetDirPath.unpack")
        zipFile = File("$targetDirPath.zip")
        etagFile = File("$targetDirPath.etag")

        return try {
            targetDir.deleteRecursively()
            unpackDir.deleteRecursively()
            unpackDir.mkdirs()

            if (!isStopped) {
                downloadArchive(url, bufferSize)
            }
            if (!isStopped) {
                unpackArchive()
            }
            if (!isStopped) {
                unpackDir.renameTo(targetDir)
                zipFile.delete()
                etagFile.delete()
            }
            Result.success()
        } catch (error: IOException) {
            Result.retry()
        }
    }

    /**
     * Downloads the contents of the [url] to `<targetDir>.zip`. Writes ETag to `<targetDir>.etag` file.
     */
    private suspend fun downloadArchive(url: String, bufferSize: Int) {
        val connection = openConnection(url)
        var readLength = 0L

        if (zipFile.exists() && etagFile.exists()) {
            readLength = zipFile.length()

            connection.setRequestProperty("Range", "bytes=$readLength-")
            connection.setRequestProperty("If-Range", etagFile.readText())
        }

        connection.inputStream.use { inputStream ->

            when (connection.responseCode) {
                HTTP_OK -> readLength = 0L
                HTTP_PARTIAL -> {}
                else -> throw IOException()
            }

            connection.getHeaderField("ETag")?.let(etagFile::writeText) ?: etagFile.delete()

            FileOutputStream(zipFile, readLength != 0L).use { outputStream ->
                val contentLength = connection.contentLength
                val buffer = ByteArray(bufferSize)

                while (!isStopped) {
                    setProgress(workDataOf(CONTENT_LENGTH to contentLength, READ_LENGTH to readLength))

                    val byteCount = inputStream.read(buffer)
                    if (byteCount == -1) {
                        break
                    }

                    readLength += byteCount
                    outputStream.write(buffer, 0, byteCount)
                }
            }
        }
    }

    /**
     * Unpacks files from [zipFile] to [unpackDir].
     */
    private fun unpackArchive() {
        val canonicalDirPath = unpackDir.canonicalPath + File.separator

        ZipInputStream(zipFile.inputStream()).use { inputStream ->
            while (!isStopped) {
                val file = File(unpackDir, inputStream.nextEntry?.name ?: break)

                // https://snyk.io/research/zip-slip-vulnerability
                if (file.canonicalPath.startsWith(canonicalDirPath)) {
                    file.parentFile?.mkdirs()
                    inputStream.copyTo(FileOutputStream(file))
                }

                inputStream.closeEntry()
            }
        }
    }
}
