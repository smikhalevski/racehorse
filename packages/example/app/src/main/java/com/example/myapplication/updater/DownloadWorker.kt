package com.example.myapplication.updater

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

open class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    val bufferSize = 8192

    var stopped = false

    companion object {
        const val E_TAG = "E_TAG"
        const val URL = "URL"
    }

    open fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient()
    }

    open fun getRequest(url: String?): Request {
        return Request.Builder().url(url).build()
    }

    override fun doWork(): Result {
        val eTag = inputData.getString(E_TAG)

        val file = File("")
        val httpClient = getOkHttpClient()
        val request = getRequest(inputData.getString(URL))

        val call = if (eTag != null && file.exists()) {
            httpClient.newCall(
                request
                    .newBuilder()
                    .addHeader("Range", "bytes=${file.length()}-")
                    .addHeader("If-Range", eTag)
                    .build()
            )
        } else {
            httpClient.newCall(request)
        }

        call.execute().use { response ->
            val code = response.code()

            if (code >= 300) {
                return Result.failure()
            }

            BufferedOutputStream(FileOutputStream(file, code == 206), bufferSize).use { output ->
                val body = response.body() ?: return Result.failure()
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    val buffer = ByteArray(bufferSize)

                    while (!stopped) {
                        val readBytes = input.read(buffer)
                        if (readBytes == -1) {
                            break
                        }

                        output.write(buffer, 0, readBytes)

                        if (totalBytes != -1L) {
                            setProgressAsync(workDataOf())
                        }
                    }
                    output.flush()
                }
            }
        }

        return Result.success()
    }

    override fun onStopped() {
        stopped = true
    }
}
