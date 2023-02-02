package org.racehorse.evergreen

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Unpacks the ZIP archive contents into a target directory.
 */
class UnpackWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    companion object {
        /**
         * The path to the ZIP archive to unpack.
         */
        const val ARCHIVE_PATHNAME = "ARCHIVE_PATHNAME"

        /**
         * The target dir to write unpacked files to.
         */
        const val TARGET_DIR = "TARGET_DIR"
    }

    override suspend fun doWork(): Result {
        val archiveFile = File(requireNotNull(inputData.getString(ARCHIVE_PATHNAME)))
        val targetDir = File(requireNotNull(inputData.getString(TARGET_DIR)))

        ZipInputStream(archiveFile.inputStream()).use { zipInputStream ->
            while (!isStopped) {
                val zipEntry = zipInputStream.nextEntry ?: break
                val file = File(targetDir, zipEntry.name)

                if (!file.canonicalPath.startsWith(targetDir.canonicalPath + File.separator) || file.exists()) {
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
