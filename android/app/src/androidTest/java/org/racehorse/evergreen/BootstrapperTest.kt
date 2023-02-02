package org.racehorse.evergreen

import android.content.Context
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BootstrapperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var tempDir = TemporaryFolder()

    private lateinit var url: String

    inner class MockBootstrapper(private val url: String) :
        Bootstrapper(context, TestLifecycleOwner(), UpdateWorker::class.java, tempDir.root) {

        override suspend fun getUpdateDescriptor() = UpdateDescriptor("0.0.0", url, true)
    }

    @Before
    fun beforeEach() {
        tempDir.create()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        )

        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use {
            it.putNextEntry(ZipEntry("file.txt"))
            it.write("value".toByteArray())
            it.closeEntry()
        }

        val server = MockWebServer().also {
            it.enqueue(MockResponse().setChunkedBody(byteArrayOutputStream.toByteArray(), 1024))
            it.play()
        }

        url = server.getUrl("/").toString()
    }

    @After
    fun afterEach() {
        tempDir.delete()
    }

    @Test
    fun testUnzipsArchive() {

        MockBootstrapper(url).start()

        Thread.sleep(2000L)
    }
}
