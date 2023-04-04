package org.racehorse.evergreen

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLConnection
import java.util.zip.ZipInputStream

/**
 * Downloads ZIP archive through the [connection] and extracts its contents to [targetDir].
 *
 * [targetDir] is created only after all operations have been completed and archive contents are ready to be consumed.
 */
internal class BundleDownload(
    private val connection: URLConnection,
    private val targetDir: File,
    private val bufferSize: Int,
    private val onProgress: (contentLength: Int, readLength: Long) -> Unit,
) {

    private val outputDir = File("$targetDir.output")
    private val etagFile = File("$targetDir.etag")
    private val zipFile = File("$targetDir.zip")

    private var isPending = false
    private var isStopped = false

    /**
     * Starts the download.
     * - If [targetDir] exists it is removed and download is restarted from scratch.
     * - If there are temporary artifacts from the previous download, then the download is resumed.
     *
     * @throws IllegalStateException If the download cannot be started after it was stopped, or because it was started
     * before.
     */
    fun start() {
        if (isPending || isStopped) {
            throw IllegalStateException("Cannot be started")
        }
        isPending = true

        targetDir.deleteRecursively()

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        download()

        if (!isStopped) {
            unzip()
        }
        if (!isStopped) {
            zipFile.delete()
            etagFile.delete()
            outputDir.renameTo(targetDir)
        }
    }

    /**
     * Stops the pending download.
     */
    fun stop() {
        isStopped = true
    }

    private fun download() {
        var readLength = 0L

        if (zipFile.exists() && etagFile.exists()) {
            readLength = zipFile.length()

            connection.setRequestProperty("Range", "bytes=$readLength-")
            connection.setRequestProperty("If-Range", etagFile.readText())
        }

        connection.inputStream.buffered().use { inputStream ->

            when ((connection as HttpURLConnection).responseCode) {

                HttpURLConnection.HTTP_OK -> readLength = 0L
                HttpURLConnection.HTTP_PARTIAL -> {}

                else -> throw IOException()
            }

            if (connection.getHeaderField("Accept-Ranges")?.contains("bytes") == true) {
                connection.getHeaderField("ETag")?.let(etagFile::writeText) ?: etagFile.delete()
            }

            FileOutputStream(zipFile, readLength != 0L).use { outputStream ->
                val contentLength = connection.contentLength
                val buffer = ByteArray(bufferSize)

                while (!isStopped) {
                    onProgress(contentLength, readLength)

                    val byteCount = inputStream.read(buffer)
                    if (byteCount == -1) {
                        break
                    }

                    readLength += byteCount
                    outputStream.write(buffer, 0, byteCount)
                }
            }
        }
    }

    private fun unzip() {
        val unzipDirPath = outputDir.canonicalPath + File.separator

        ZipInputStream(zipFile.inputStream()).use { inputStream ->
            while (!isStopped) {
                val file = File(outputDir, inputStream.nextEntry?.name ?: break)

                // https://snyk.io/research/zip-slip-vulnerability
                if (file.canonicalPath.startsWith(unzipDirPath)) {
                    file.parentFile?.mkdirs()
                    inputStream.copyTo(FileOutputStream(file))
                }

                inputStream.closeEntry()
            }
        }
    }
}
