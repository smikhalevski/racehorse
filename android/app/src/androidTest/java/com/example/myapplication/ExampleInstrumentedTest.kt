package com.example.myapplication

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestWorkerBuilder
import androidx.work.workDataOf
import com.example.myapplication.updater.DownloadWorker
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.myapplication", appContext.packageName)
    }

    private lateinit var appContext: Context
    private lateinit var executor: Executor

    @Before
    fun startUp() {
        appContext = ApplicationProvider.getApplicationContext()
        executor = Executors.newSingleThreadExecutor()
    }

    @Test
    fun testDownloadWorker() {
        val server = MockWebServer()

        server.enqueue(MockResponse().setBody("hello, world!"))

        server.play()

        val worker = TestWorkerBuilder<DownloadWorker>(appContext, executor)
            .setInputData(
                workDataOf(
                    DownloadWorker.URL to server.getUrl("/").toString(),
                    DownloadWorker.PATHNAME to File(appContext.filesDir, "ccc.txt").absolutePath
                )
            )
            .build()

        val result = worker.doWork()

        assertEquals(result.outputData.getInt(DownloadWorker.PERCENTAGE, 0), 100)
    }
}
