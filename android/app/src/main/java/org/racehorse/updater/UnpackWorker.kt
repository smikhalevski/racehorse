package org.racehorse.updater

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Unpacks the ZIP archive contents into a given directory.
 */
class UnpackWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        const val ARCHIVE_PATHNAME = "ARCHIVE_PATHNAME"
        const val TARGET_DIR = "TARGET_DIR"
    }

    override suspend fun doWork(): Result {
        val archiveFile = File(requireNotNull(inputData.getString(ARCHIVE_PATHNAME)))
        val targetDir = File(requireNotNull(inputData.getString(TARGET_DIR)))

        ZipInputStream(archiveFile.inputStream()).use { zipInputStream ->
            while (!isStopped) {
                val zipEntry = zipInputStream.nextEntry ?: break
                val file = File(targetDir, zipEntry.name)

                if (!file.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                    throw SecurityException("ZIP entry $zipEntry is outside of the target directory $targetDir")
                }
                if (file.exists()) {
                    continue
                }

                file.parentFile?.mkdirs()
                zipInputStream.copyTo(FileOutputStream(file))
            }
            zipInputStream.closeEntry()
        }

        return Result.success()
    }
}
