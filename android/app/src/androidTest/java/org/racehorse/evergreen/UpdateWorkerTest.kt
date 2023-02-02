package org.racehorse.evergreen

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import com.squareup.okhttp.mockwebserver.MockResponse
import com.squareup.okhttp.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class UpdateWorkerTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var tempDir = TemporaryFolder()

    private lateinit var targetDir: File
    private lateinit var unpackDir: File
    private lateinit var zipFile: File
    private lateinit var etagFile: File

    @Before
    fun beforeEach() {
        tempDir.create()

        targetDir = tempDir.root
        unpackDir = File("${targetDir.absolutePath}.unpack")
        zipFile = File("${targetDir.absolutePath}.zip")
        etagFile = File("${targetDir.absolutePath}.etag")
    }

    @After
    fun afterEach() {
        tempDir.delete()
    }

    @Test
    fun testDownloadsAndSafelyUnpacksZipArchive() {
        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use {
            it.putNextEntry(ZipEntry("file1.txt"))
            it.write("value1".toByteArray())
            it.closeEntry()

            it.putNextEntry(ZipEntry("../file2.txt"))
            it.write("value2".toByteArray())
            it.closeEntry()

            it.putNextEntry(ZipEntry("dir/file3.txt"))
            it.write("value3".toByteArray())
            it.closeEntry()
        }

        val server = MockWebServer().also {
            it.enqueue(
                MockResponse()
                    .setResponseCode(HTTP_OK)
                    .setChunkedBody(byteArrayOutputStream.toByteArray(), 1024)
            )
            it.play()
        }

        val worker = TestListenableWorkerBuilder<UpdateWorker>(context)
            .setInputData(
                workDataOf(
                    UpdateWorker.URL to server.getUrl("/").toString(),
                    UpdateWorker.TARGET_DIR to targetDir.absolutePath,
                )
            )
            .build()

        val result = runBlocking {
            worker.doWork()
        }

        Assert.assertTrue("result", result is ListenableWorker.Result.Success)

        Assert.assertEquals("value1", File(targetDir, "file1.txt").readText())
        Assert.assertEquals("value3", File(targetDir, "dir/file3.txt").readText())

        Assert.assertFalse("zip slip", File(targetDir, "../file2.txt").exists())

        Assert.assertFalse("unpackDir", unpackDir.exists())
        Assert.assertFalse("zipFile", zipFile.exists())
        Assert.assertFalse("etagFile", etagFile.exists())
    }

    @Test
    fun testOverwritesTargetDir() {
        File(targetDir, "delete.me").writeText("okay")

        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use {
            it.putNextEntry(ZipEntry("file.txt"))
            it.write("value".toByteArray())
            it.closeEntry()
        }

        val server = MockWebServer().also {
            it.enqueue(
                MockResponse()
                    .setResponseCode(HTTP_OK)
                    .setChunkedBody(byteArrayOutputStream.toByteArray(), 1024)
            )
            it.play()
        }

        val worker = TestListenableWorkerBuilder<UpdateWorker>(context)
            .setInputData(
                workDataOf(
                    UpdateWorker.URL to server.getUrl("/").toString(),
                    UpdateWorker.TARGET_DIR to targetDir.absolutePath,
                )
            )
            .build()

        val result = runBlocking {
            worker.doWork()
        }

        Assert.assertTrue("result", result is ListenableWorker.Result.Success)

        Assert.assertEquals("value", File(targetDir, "file.txt").readText())

        Assert.assertFalse("deleted file", File(targetDir, "delete.me").exists())
    }

    @Test
    fun testResumesDownload() {
        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use {
            it.putNextEntry(ZipEntry("file1.txt"))
            it.write("value1".toByteArray())
            it.closeEntry()

            it.putNextEntry(ZipEntry("file2.txt"))
            it.write("value2".toByteArray())
            it.closeEntry()
        }

        val byteArray = byteArrayOutputStream.toByteArray()

        zipFile.writeBytes(byteArray.sliceArray(0..byteArray.size / 2))
        etagFile.writeText("33a64df5514")

        val server = MockWebServer().also {
            it.enqueue(
                MockResponse()
                    .setResponseCode(HTTP_PARTIAL)
                    .setChunkedBody(byteArray.sliceArray(byteArray.size / 2 until byteArray.size), 1024)
            )
            it.play()
        }

        val worker = TestListenableWorkerBuilder<UpdateWorker>(context)
            .setInputData(
                workDataOf(
                    UpdateWorker.URL to server.getUrl("/").toString(),
                    UpdateWorker.TARGET_DIR to targetDir.absolutePath,
                )
            )
            .build()

        val result = runBlocking {
            worker.doWork()
        }

        val request = server.takeRequest()

        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("bytes=130-", request.getHeader("Range"))
        Assert.assertEquals("33a64df5514", request.getHeader("If-Range"))

        Assert.assertTrue("result", result is ListenableWorker.Result.Success)

        Assert.assertEquals("value1", File(targetDir, "file1.txt").readText())
        Assert.assertEquals("value2", File(targetDir, "file2.txt").readText())
    }

    @Test
    fun testPreservesDownloadedDataAfterStop() {
        val byteArrayOutputStream = ByteArrayOutputStream()

        ZipOutputStream(byteArrayOutputStream).use {
            it.putNextEntry(ZipEntry("file1.txt"))
            it.write("value1".toByteArray())
            it.closeEntry()

            it.putNextEntry(ZipEntry("file2.txt"))
            it.write("value2".toByteArray())
            it.closeEntry()
        }

        val byteArray = byteArrayOutputStream.toByteArray()

        val server = MockWebServer().also {
            it.enqueue(
                MockResponse()
                    .setResponseCode(HTTP_OK)
                    .setHeader("ETag", "33a64df5514")
                    .setChunkedBody(byteArray, 1024)
                    .setBytesPerSecond(36)
            )
            it.play()
        }

        val worker = TestListenableWorkerBuilder<UpdateWorker>(context)
            .setInputData(
                workDataOf(
                    UpdateWorker.URL to server.getUrl("/").toString(),
                    UpdateWorker.TARGET_DIR to targetDir.absolutePath,
                    UpdateWorker.BUFFER_SIZE to 28
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

        Assert.assertEquals("zip length", 95, zipFile.length())
        Assert.assertArrayEquals("zip bytes", byteArray.sliceArray(0..94), zipFile.readBytes())
        Assert.assertEquals("33a64df5514", etagFile.readText())
    }
}
