package org.racehorse.evergreen

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class DownloadWorkerTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    private val outputFile = File(appContext.filesDir, "output.txt")
    private val partialFile = File(outputFile.absolutePath + DownloadWorker.PARTIAL_SUFFIX)
    private val etagFile = File(outputFile.absolutePath + DownloadWorker.ETAG_SUFFIX)

    @Before
    fun setup() {
        DownloadWorker.deleteDownload(outputFile.absolutePath)
    }

    @Test
    fun testDownloadsFile() {
        val server = MockWebServer()

        server.enqueue(MockResponse().setBody("xxx"))

        server.play()

        val worker = TestListenableWorkerBuilder<DownloadWorker>(appContext)
            .setInputData(
                workDataOf(
                    DownloadWorker.URL to server.getUrl("/").toString(),
                    DownloadWorker.PATHNAME to outputFile.absolutePath,
                )
            )
            .build()

        val result = runBlocking {
            worker.doWork()
        }

        Assert.assertTrue(result is ListenableWorker.Result.Success)
        Assert.assertTrue(outputFile.exists())
        Assert.assertEquals("xxx", outputFile.readText())
    }

    @Test
    fun testOverwritesPreviouslyDownloadedFile() {
        outputFile.writeText("xxx")

        Assert.assertEquals("xxx", outputFile.readText())

        val server = MockWebServer()

        server.enqueue(MockResponse().setBody("yyy"))

        server.play()

        val worker = TestListenableWorkerBuilder<DownloadWorker>(appContext)
            .setInputData(
                workDataOf(
                    DownloadWorker.URL to server.getUrl("/").toString(),
                    DownloadWorker.PATHNAME to outputFile.absolutePath,
                )
            )
            .build()

        val result = runBlocking {
            worker.doWork()
        }

        Assert.assertTrue(result is ListenableWorker.Result.Success)
        Assert.assertEquals("yyy", outputFile.readText())
    }

    @Test
    fun testResumesPartialDownload() {
        partialFile.writeText("111_22")
        etagFile.writeText("bbb")

        val server = MockWebServer()

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_PARTIAL)
                .setHeader("ETag", "bbb")
                .setBody("2_333")
        )
        server.play()

        val worker = TestListenableWorkerBuilder<DownloadWorker>(appContext)
            .setInputData(
                workDataOf(
                    DownloadWorker.URL to server.getUrl("/").toString(),
                    DownloadWorker.PATHNAME to outputFile.absolutePath,
                    DownloadWorker.BUFFER_SIZE to 4
                )
            )
            .build()

        val result = runBlocking {
            worker.doWork()
        }

        val request = server.takeRequest()

        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("bytes=6-", request.getHeader("Range"))
        Assert.assertEquals("bbb", request.getHeader("If-Range"))

        Assert.assertTrue(result is ListenableWorker.Result.Success)
        Assert.assertTrue(outputFile.exists())
        Assert.assertEquals("111_222_333", outputFile.readText())
    }

    @Test
    fun testPreservesPartialDownloadAfterStop() {
        val server = MockWebServer()

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("ETag", "bbb")
                .setBody("111_222_333")
                .setBytesPerSecond(2)
        )

        server.play()

        val worker = TestListenableWorkerBuilder<DownloadWorker>(appContext)
            .setInputData(
                workDataOf(
                    DownloadWorker.URL to server.getUrl("/").toString(),
                    DownloadWorker.PATHNAME to outputFile.absolutePath,
                    DownloadWorker.BUFFER_SIZE to 2
                )
            )
            .build()

        runBlocking {
            thread {
                Thread.sleep(1500L)
                worker.stop()
            }
            worker.doWork()
        }

        val request = server.takeRequest()

        Assert.assertEquals("GET", request.method)
        Assert.assertNull(request.getHeader("Range"))
        Assert.assertNull(request.getHeader("If-Range"))

        Assert.assertFalse(outputFile.exists())
        Assert.assertEquals("111_22", partialFile.readText())
        Assert.assertEquals("bbb", etagFile.readText())
    }
}
