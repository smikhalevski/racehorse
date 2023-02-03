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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BundleDownloadTest {
    private var tempDir = TemporaryFolder()

    private lateinit var targetDir: File
    private lateinit var outputDir: File
    private lateinit var etagFile: File
    private lateinit var zipFile: File

    private lateinit var server: MockWebServer

    @Before
    fun beforeEach() {
        tempDir.create()

        targetDir = tempDir.root
        outputDir = File("$targetDir.output")
        etagFile = File("$targetDir.etag")
        zipFile = File("$targetDir.zip")

        server = MockWebServer().also { it.play() }
    }

    @After
    fun afterEach() {
        tempDir.delete()
        server.shutdown()
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

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setChunkedBody(byteArrayOutputStream.toByteArray(), 1024)
        )

        BundleDownload(server.getUrl("/").openConnection(), targetDir, 1024) { contentLength, readLength -> }.start()

        Assert.assertEquals("value1", File(targetDir, "file1.txt").readText())
        Assert.assertEquals("value3", File(targetDir, "dir/file3.txt").readText())

        Assert.assertFalse("zip slip", File(targetDir, "../file2.txt").exists())

        Assert.assertFalse("unpackDir", outputDir.exists())
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

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setChunkedBody(byteArrayOutputStream.toByteArray(), 1024)
        )

        BundleDownload(server.getUrl("/").openConnection(), targetDir, 1024) { contentLength, readLength -> }.start()

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

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_PARTIAL)
                .setChunkedBody(byteArray.sliceArray(byteArray.size / 2 until byteArray.size), 1024)
        )

        BundleDownload(server.getUrl("/").openConnection(), targetDir, 1024) { contentLength, readLength -> }.start()

        val request = server.takeRequest()

        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("bytes=130-", request.getHeader("Range"))
        Assert.assertEquals("33a64df5514", request.getHeader("If-Range"))

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

        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setHeader("ETag", "33a64df5514")
                .setChunkedBody(byteArray, 1024)
                .setBytesPerSecond(36)
        )

        val download =
            BundleDownload(server.getUrl("/").openConnection(), targetDir, 28) { contentLength, readLength -> }

        Thread {
            Thread.sleep(1500L)
            download.stop()
        }.start()

        download.start()

        Assert.assertEquals("zip length", 95, zipFile.length())
        Assert.assertArrayEquals("zip bytes", byteArray.sliceArray(0..94), zipFile.readBytes())
        Assert.assertEquals("33a64df5514", etagFile.readText())
    }
}
