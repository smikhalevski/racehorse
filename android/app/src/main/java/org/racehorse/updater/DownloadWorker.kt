package org.racehorse.updater

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
 * Downloads URL to a file, can continue previously started download if server responded with ETag header.
 */
open class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        /**
         * The bundle archive URL.
         */
        const val URL = "URL"

        /**
         * The pathname where the bundle archive should be saved.
         */
        const val PATHNAME = "PATHNAME"

        /**
         * The number of bytes to read in one chunk.
         */
        const val BUFFER_SIZE = "BUFFER_SIZE"

        /**
         * The total number of bytes in the downloaded file.
         */
        const val CONTENT_LENGTH = "CONTENT_LENGTH"

        /**
         * The total number of bytes read.
         */
        const val READ_LENGTH = "READ_LENGTH"
    }

    open fun openConnection(url: String): HttpURLConnection {
        return URL(url).openConnection() as HttpURLConnection
    }

    override suspend fun doWork(): Result {
        val url = requireNotNull(inputData.getString(URL))
        val pathname = requireNotNull(inputData.getString(PATHNAME))
        val bufferSize = inputData.getInt(BUFFER_SIZE, 8192)

        val tempFile = File("$pathname.temp")
        val etagFile = File("$pathname.etag")

        var readLength = 0L
        val connection = openConnection(url)

        if (tempFile.exists() && etagFile.exists()) {
            readLength = tempFile.length()
            connection.setRequestProperty("Range", "bytes=$readLength-")
            connection.setRequestProperty("If-Range", etagFile.readText())
        }

        return try {
            connection.inputStream.use { inputStream ->
                val etag = connection.getHeaderField("ETag")
                val partial = connection.responseCode == HttpURLConnection.HTTP_PARTIAL

                if (!partial) {
                    readLength = 0L
                }
                if (etag != null) {
                    etagFile.writeText(etag)
                }

                FileOutputStream(tempFile, partial).use { outputStream ->
                    val contentLength = connection.contentLength
                    val buffer = ByteArray(bufferSize)

                    while (!this@DownloadWorker.isStopped) {
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

            tempFile.renameTo(File(pathname))
            etagFile.delete()

            return Result.success(workDataOf(CONTENT_LENGTH to readLength, READ_LENGTH to readLength))
        } catch (_: IOException) {
            Result.retry()
        }
    }
}
