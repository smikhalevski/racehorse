package com.example.myapplication.updater

import android.content.Context
import android.os.Build.VERSION
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

open class DownloadWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    open val bufferSize = 8192

    private var stopped = false

    companion object {
        const val VERSION = "VERSION"
        const val URL = "URL"
        const val PATHNAME = "PATHNAME"
        const val E_TAG = "E_TAG"
        const val PERCENTAGE = "PERCENTAGE"
    }

    open fun openConnection(url: String): HttpURLConnection {
        return URL(url).openConnection() as HttpURLConnection
    }

    override fun doWork(): Result {
        val version = inputData.getString(VERSION) ?: throw IllegalArgumentException("Expected $VERSION")
        val url = inputData.getString(URL) ?: throw IllegalArgumentException("Expected $URL")
        val pathname = inputData.getString(PATHNAME) ?: throw IllegalArgumentException("Expected $PATHNAME")

        setProgressAsync(workDataOf(VERSION to version, PERCENTAGE to 0))

        val connection = openConnection(url)
        val file = File(pathname)
        val eTag = inputData.getString(E_TAG)

        var percentage = inputData.getInt(PERCENTAGE, 0)
        var readOffset = 0L

        if (eTag != null && file.exists()) {
            readOffset = file.length()

            connection.setRequestProperty("Range", "bytes=$readOffset-")
            connection.setRequestProperty("If-Range", eTag)
        }

        return try {
            connection.inputStream.use { inputStream ->
                setProgressAsync(workDataOf(E_TAG to connection.getHeaderField("ETag")))

                FileOutputStream(file, connection.responseCode == HttpURLConnection.HTTP_PARTIAL).use { outputStream ->
                    val contentLength = connection.contentLength
                    val buffer = ByteArray(bufferSize)

                    while (!stopped) {
                        val byteCount = inputStream.read(buffer)
                        if (byteCount == -1) {
                            break
                        }

                        readOffset += byteCount
                        outputStream.write(buffer, 0, byteCount)

                        val readPercentage = readOffset * 100 / contentLength

                        if (readPercentage > percentage) {
                            percentage = readPercentage.toInt()
                            setProgressAsync(workDataOf(PERCENTAGE to percentage))
                        }
                    }
                }
            }
            return Result.success(workDataOf(PERCENTAGE to 100))
        } catch (_: IOException) {
            Result.retry()
        }
    }

    override fun onStopped() {
        stopped = true
    }
}
