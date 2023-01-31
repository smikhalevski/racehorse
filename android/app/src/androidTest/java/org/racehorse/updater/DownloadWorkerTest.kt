package org.racehorse.updater

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class DownloadWorkerTest {

    @Test
    fun testUninterruptedDownloadSucceeds() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val responseBody = "aaa_aaa_aaa_aaa"
        val outputFile = File(appContext.filesDir, "ccc.txt")

        val server = MockWebServer()

        server.enqueue(MockResponse().setBody(responseBody).setHeader("ETag", "bbb"))
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

        runBlocking {
            val result = worker.doWork()

            Assert.assertTrue(result is ListenableWorker.Result.Success)
            Assert.assertEquals(15, result.outputData.getLong(DownloadWorker.CONTENT_LENGTH, 0))
            Assert.assertEquals(15, result.outputData.getLong(DownloadWorker.READ_LENGTH, 0))
            Assert.assertEquals(true, outputFile.exists())
            Assert.assertEquals(responseBody, outputFile.readText())
        }
    }
}
