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
 * - [targetDir] is _always_ deleted when [start] is called.
 * - [targetDir] is created only after all operations have been completed and archive contents are ready to be consumed.
 * - If there are temporary artifacts from the previous download, then the download is resumed.
 *
 * @param connection The connection from which ZIP archive bytes are read.
 * @param targetDir The directory to which contents of the ZIP archive are written.
 * @param bufferSize The size of the buffer into which data is read from the [connection] stream.
 * @param onProgress The callback that reports progress of the download.
 */
internal class BundleDownload(
    private val connection: URLConnection,
    private val targetDir: File,
    private val bufferSize: Int,
    private val onProgress: (contentLength: Int, readLength: Long) -> Unit,
) {

    /**
     * The directory to which archive contents are unzipped.
     */
    private val unzipDir = File("$targetDir.unzip")

    /**
     * The file with the ETag of the [zipFile] that is being downloaded.
     */
    private val etagFile = File("$targetDir.etag")

    /**
     * The file that is being downloaded.
     */
    private val zipFile = File("$targetDir.zip")

    private var isPending = false
    private var isStopped = false

    /**
     * Starts the download.
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

        unzipDir.deleteRecursively()
        unzipDir.mkdirs()

        download()

        if (!isStopped) {
            unzip()
        }
        if (!isStopped) {
            zipFile.delete()
            etagFile.delete()
            unzipDir.renameTo(targetDir)
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

                else -> throw IOException("Cannot download bundle")
            }

            if (connection.getHeaderField("Accept-Ranges")?.contains("bytes") == true) {
                connection.getHeaderField("ETag")?.let(etagFile::writeText) ?: etagFile.delete()
            }

            FileOutputStream(zipFile, readLength != 0L).use { outputStream ->
                val contentLength = connection.contentLength.let { if (it != -1) it + readLength.toInt() else -1 }
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
        val unzipDirPath = unzipDir.canonicalPath + File.separator

        ZipInputStream(zipFile.inputStream()).use { inputStream ->
            while (!isStopped) {
                val file = File(unzipDir, inputStream.nextEntry?.name ?: break)

                // https://snyk.io/research/zip-slip-vulnerability
                if (file.canonicalPath.startsWith(unzipDirPath)) {
                    file.parentFile!!.mkdirs()
                    inputStream.copyTo(FileOutputStream(file))
                }

                inputStream.closeEntry()
            }
        }
    }
}
