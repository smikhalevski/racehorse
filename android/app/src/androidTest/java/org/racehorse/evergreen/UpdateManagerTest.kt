package org.racehorse.evergreen

import android.util.Log
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpdateManagerTest {

    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(appContext, config)
    }

    inner class MockUpdateManager(private val bundleUrl: String) : UpdateManager(
        appContext,
        TestLifecycleOwner(),
        DownloadWorker::class.java
    ) {
        override suspend fun getUpdateDescriptor() = UpdateDescriptor("x.x.x", bundleUrl, true)
    }

    @Test
    fun testUnzipsArchive() {
        val server = MockWebServer()

        server.enqueue(MockResponse().setBody("xxx"))

        server.play()

        runBlocking {
            MockUpdateManager(server.getUrl("/").toString()).start()
        }
    }
}
