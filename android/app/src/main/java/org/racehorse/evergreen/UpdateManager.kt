package org.racehorse.evergreen

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit


abstract class UpdateManager(
    private val appContext: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val downloadWorkerClass: Class<out DownloadWorker>
) {

    companion object {
        const val DOWNLOAD_NEXT_BUNDLE = "DOWNLOAD_NEXT_BUNDLE"
    }

    private var currentBundleDir: File = File(appContext.filesDir, "current")
    private var currentVersionFile: File = File(appContext.filesDir, "current.version")

    private var nextBundleDir: File = File(appContext.filesDir, "next")
    private var nextBundleArchiveFile: File = File(appContext.filesDir, "next.zip")
    private var nextVersionFile: File = File(appContext.filesDir, "next.version")

    protected abstract suspend fun getUpdateDescriptor(): UpdateDescriptor

    protected open fun onUpToDate() {}

    protected open fun onNonBlockingUpdateStarted() {}

    protected open fun onProgress(contentLength: Int, readLength: Int) {}

    protected open fun onBundleDownloaded() {}

    suspend fun start() {
        val updateDescriptor = getUpdateDescriptor()
        val currentVersion = if (currentVersionFile.exists()) currentVersionFile.readText() else null
        val nextVersion = if (nextVersionFile.exists()) nextVersionFile.readText() else null

        if (currentVersion == updateDescriptor.version) {
            onUpToDate()
            return
        }

        if (nextVersion == updateDescriptor.version) {
            if (updateDescriptor.blocking || currentVersion == null) {
                resumeNextBundleDownload(updateDescriptor.url, true)
                return
            }

            resumeNextBundleDownload(updateDescriptor.url, false)
            onNonBlockingUpdateStarted()
            return
        }

        nextVersionFile.writeText(updateDescriptor.version)

        if (updateDescriptor.blocking || currentVersion == null) {
            downloadNextBundle(updateDescriptor.url, true)
            return
        }

        downloadNextBundle(updateDescriptor.url, false)
        onNonBlockingUpdateStarted()
    }

    /**
     * If the download is pending then join it, otherwise, start a new download.
     */
    private suspend fun resumeNextBundleDownload(url: String, moveAfterComplete: Boolean) {
        val workManager = WorkManager.getInstance(appContext)

        val infos = workManager.getWorkInfosForUniqueWork(DOWNLOAD_NEXT_BUNDLE).await()

        if (infos.size != 2 || infos[0].state == WorkInfo.State.CANCELLED || infos[1].state == WorkInfo.State.CANCELLED) {
            downloadNextBundle(url, moveAfterComplete)
            return
        }

        if (infos[0].state == WorkInfo.State.SUCCEEDED && infos[1].state == WorkInfo.State.SUCCEEDED) {
            if (moveAfterComplete) {
                moveNextBundleToCurrent()
            }
            return
        }

        observeDownloadWork(
            workManager.getWorkInfoByIdLiveData(infos[0].id),
            workManager.getWorkInfoByIdLiveData(infos[1].id),
            moveAfterComplete
        )
    }

    /**
     * Remove downloaded archive and restart the download.
     */
    private suspend fun downloadNextBundle(url: String, moveAfterComplete: Boolean) {
        nextBundleDir.deleteRecursively()
        DownloadWorker.deleteDownload(nextBundleArchiveFile.absolutePath)

        val workManager = WorkManager.getInstance(appContext)

        val downloadRequest = OneTimeWorkRequest.Builder(downloadWorkerClass)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                workDataOf(
                    DownloadWorker.URL to url,
                    DownloadWorker.PATHNAME to nextBundleArchiveFile.absolutePath
                )
            )
            .build()

        val unpackRequest = OneTimeWorkRequestBuilder<UnpackWorker>()
            .setInputData(
                workDataOf(
                    UnpackWorker.ARCHIVE_PATHNAME to nextBundleArchiveFile.absolutePath,
                    UnpackWorker.TARGET_DIR to nextBundleDir.absolutePath
                )
            )
            .build()

        workManager
            .beginUniqueWork(DOWNLOAD_NEXT_BUNDLE, ExistingWorkPolicy.REPLACE, downloadRequest)
            .then(unpackRequest)
            .enqueue()

        observeDownloadWork(
            workManager.getWorkInfoByIdLiveData(downloadRequest.id),
            workManager.getWorkInfoByIdLiveData(unpackRequest.id),
            moveAfterComplete
        )
    }

    private suspend fun observeDownloadWork(
        downloadLiveData: LiveData<WorkInfo>,
        unpackLiveData: LiveData<WorkInfo>,
        moveAfterComplete: Boolean
    ) {
        withContext(Dispatchers.Main) {
            downloadLiveData.observe(lifecycleOwner) {
                if (it.state == WorkInfo.State.RUNNING) {
                    onProgress(
                        it.progress.getInt(DownloadWorker.CONTENT_LENGTH, 0),
                        it.progress.getInt(DownloadWorker.READ_LENGTH, 0)
                    )
                }
            }

            unpackLiveData.observe(lifecycleOwner) {
                if (it.state == WorkInfo.State.SUCCEEDED) {
                    if (moveAfterComplete) {
                        moveNextBundleToCurrent()
                    }
                    onBundleDownloaded()
                }
            }
        }
    }

    private fun moveNextBundleToCurrent() {
        currentBundleDir.deleteRecursively()
        currentVersionFile.delete()

        nextBundleDir.renameTo(currentBundleDir)
        nextVersionFile.renameTo(currentVersionFile)
    }
}
