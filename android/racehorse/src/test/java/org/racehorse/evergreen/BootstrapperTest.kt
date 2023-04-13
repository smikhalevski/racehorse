package org.racehorse.evergreen

import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BootstrapperTest {
    private var tempDir = TemporaryFolder()

    private lateinit var masterDir: File
    private lateinit var masterVersionFile: File
    private lateinit var updateDir: File
    private lateinit var updateVersionFile: File

    private lateinit var server: MockWebServer

    private val callIndex = AtomicInteger()
    private var bundleReadyCallIndex = -1
    private var updateStartedCallIndex = -1
    private var updateFailedCallIndex = -1
    private var updateReadyCallIndex = -1
    private var updateProgressCallIndex = -1

    inner class MockBootstrapper : Bootstrapper(tempDir.root) {
        override fun onBundleReady(appDir: File) {
            bundleReadyCallIndex = callIndex.incrementAndGet()
        }

        override fun onUpdateStarted(mandatory: Boolean) {
            updateStartedCallIndex = callIndex.incrementAndGet()
        }

        override fun onUpdateFailed(mandatory: Boolean, cause: Throwable) {
            updateFailedCallIndex = callIndex.incrementAndGet()
        }

        override fun onUpdateReady(version: String) {
            updateReadyCallIndex = callIndex.incrementAndGet()
        }

        override fun onUpdateProgress(contentLength: Int, readLength: Long) {
            updateProgressCallIndex = callIndex.incrementAndGet()
        }
    }

    @Before
    fun beforeEach() {
        tempDir.create()

        masterDir = File(tempDir.root, "master")
        masterVersionFile = File(tempDir.root, "master.version")
        updateDir = File(tempDir.root, "update")
        updateVersionFile = File(tempDir.root, "update.version")

        server = MockWebServer().also { it.play() }

        callIndex.set(0)
        bundleReadyCallIndex = -1
        updateStartedCallIndex = -1
        updateFailedCallIndex = -1
        updateReadyCallIndex = -1
        updateProgressCallIndex = -1
    }

    @After
    fun afterEach() {
        tempDir.delete()
        server.shutdown()
    }

    @Test
    fun testDownloadsMainBundleForBlankApp() {
        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use {
            it.putNextEntry(ZipEntry("file.txt"))
            it.write("value".toByteArray())
            it.closeEntry()
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setChunkedBody(byteArrayOutputStream.toByteArray(), 1024)
        )

        MockBootstrapper().start("0.0.0", false) {
            server.getUrl("/").openConnection()
        }

        Assert.assertEquals("value", File(masterDir, "file.txt").readText())
        Assert.assertEquals("0.0.0", masterVersionFile.readText())

        Assert.assertFalse("no update dir", updateDir.exists())
        Assert.assertFalse("no update version", updateVersionFile.exists())

        Assert.assertEquals("onBundleReadyCall index", 4, bundleReadyCallIndex)
        Assert.assertEquals("onUpdateStartedCall index", 1, updateStartedCallIndex)
        Assert.assertEquals("onUpdateFailedCall index", -1, updateFailedCallIndex)
        Assert.assertEquals("onUpdateReadyCall index", -1, updateReadyCallIndex)
        Assert.assertEquals("onUpdateProgressCall index", 3, updateProgressCallIndex)
    }

    @Test
    fun testAppliesExistingUpdate() {
        updateDir.mkdirs()

        File(updateDir, "file.txt").writeText("value")
        updateVersionFile.writeText("1.1.1")

        MockBootstrapper().start("1.1.1", false) {
            throw Exception()
        }

        Assert.assertEquals("value", File(masterDir, "file.txt").readText())
        Assert.assertEquals("1.1.1", masterVersionFile.readText())

        Assert.assertFalse("no update dir", updateDir.exists())
        Assert.assertFalse("no update version", updateVersionFile.exists())

        Assert.assertEquals("onBundleReadyCall index", 1, bundleReadyCallIndex)
        Assert.assertEquals("onUpdateStartedCall index", -1, updateStartedCallIndex)
        Assert.assertEquals("onUpdateFailedCall index", -1, updateFailedCallIndex)
        Assert.assertEquals("onUpdateReadyCall index", -1, updateReadyCallIndex)
        Assert.assertEquals("onUpdateProgressCall index", -1, updateProgressCallIndex)
    }

    @Test
    fun testIgnoresExistingUpdateOnVersionMismatch() {
        updateDir.mkdirs()

        File(updateDir, "file1.txt").writeText("value1")
        updateVersionFile.writeText("1.1.1")

        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use {
            it.putNextEntry(ZipEntry("file2.txt"))
            it.write("value2".toByteArray())
            it.closeEntry()
        }

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setChunkedBody(byteArrayOutputStream.toByteArray(), 1024)
        )

        MockBootstrapper().start("2.2.2", false) {
            server.getUrl("/").openConnection()
        }

        Assert.assertEquals("value2", File(masterDir, "file2.txt").readText())
        Assert.assertEquals("2.2.2", masterVersionFile.readText())

        Assert.assertFalse("no update dir", updateDir.exists())
        Assert.assertFalse("no update version", updateVersionFile.exists())

        Assert.assertEquals("onBundleReadyCall index", 4, bundleReadyCallIndex)
        Assert.assertEquals("onUpdateStartedCall index", 1, updateStartedCallIndex)
        Assert.assertEquals("onUpdateFailedCall index", -1, updateFailedCallIndex)
        Assert.assertEquals("onUpdateReadyCall index", -1, updateReadyCallIndex)
        Assert.assertEquals("onUpdateProgressCall index", 3, updateProgressCallIndex)
    }
}
