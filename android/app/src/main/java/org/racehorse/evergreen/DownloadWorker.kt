package org.racehorse.evergreen

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Downloads URL response body to a local file at given pathname, can continue previously started download if server
 * responded with ETag header.
 */
open class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val URL = "URL"
        const val PATHNAME = "PATHNAME"
        const val BUFFER_SIZE = "BUFFER_SIZE"
        const val CONTENT_LENGTH = "CONTENT_LENGTH"
        const val READ_LENGTH = "READ_LENGTH"

        const val PARTIAL_SUFFIX = ".partial"
        const val ETAG_SUFFIX = ".etag"

        fun deleteDownload(pathname: String) {
            File(pathname).delete()
            File(pathname + PARTIAL_SUFFIX).delete()
            File(pathname + ETAG_SUFFIX).delete()
        }
    }

    open fun openConnection(url: String): HttpURLConnection {
        return URL(url).openConnection() as HttpURLConnection
    }

    override suspend fun doWork() = try {
        downloadFile()
    } catch (e: IOException) {
        Result.retry()
    }

    private suspend fun downloadFile(): Result {
        val url = requireNotNull(inputData.getString(URL))
        val pathname = requireNotNull(inputData.getString(PATHNAME))
        val bufferSize = inputData.getInt(BUFFER_SIZE, 8192)

        val partialFile = File(pathname + PARTIAL_SUFFIX)
        val etagFile = File(pathname + ETAG_SUFFIX)

        var readLength = 0
        val connection = openConnection(url)

        if (partialFile.exists() && etagFile.exists()) {
            readLength = partialFile.length().toInt()
            connection.setRequestProperty("Range", "bytes=$readLength-")
            connection.setRequestProperty("If-Range", etagFile.readText())
        }

        connection.inputStream.use { inputStream ->
            if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL && connection.responseCode != HttpURLConnection.HTTP_OK) {
                return Result.retry()
            }
            if (connection.responseCode != HttpURLConnection.HTTP_PARTIAL) {
                readLength = 0
            }

            val etag = connection.getHeaderField("ETag")
            if (etag != null) {
                etagFile.writeText(etag)
            }

            FileOutputStream(partialFile, readLength != 0).use { outputStream ->
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

        if (!isStopped && partialFile.renameTo(File(pathname))) {
            etagFile.delete()
            return Result.success()
        }

        return Result.failure()
    }
}
