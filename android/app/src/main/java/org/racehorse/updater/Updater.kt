package org.racehorse.updater

import android.content.Context
import androidx.lifecycle.Observer
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

open class Updater(
    private val updateDescriptorUrl: String,
    private val baseDirectory: File,
    private val appContext: Context
) {

    companion object {
        const val DOWNLOAD_BUNDLE = "DOWNLOAD_BUNDLE"
    }

    private val progressObserver = Observer<WorkInfo> {
        val progress = 0//it.progress.getInt(DownloadWorker.PERCENTAGE, 0)

        onProgress(progress)

        if (progress == 100) {
            onBundleDownloaded()
        }
    }

    fun onUpToDate() {}

    fun onNonBlockingUpdateStarted() {}

    fun onProgress(progress: Int) {}

    fun onBundleDownloaded() {}

    fun update() {
        val updateDescriptor = readUpdateDescriptor(updateDescriptorUrl)
    }

    private suspend fun downloadBundle(version: String, url: String, pathname: String) {
        val workManager = WorkManager.getInstance(appContext)

        val workInfos = workManager.getWorkInfosForUniqueWork(DOWNLOAD_BUNDLE).await()

        val workRequestId =
            if (workInfos.isEmpty() /*|| workInfos[0].progress.getString(DownloadWorker.VERSION) != version*/) {

                val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
                    .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                    )
                    .setInputData(
                        workDataOf(
                            DownloadWorker.URL to url,
                            DownloadWorker.PATHNAME to pathname
                        )
                    )
                    .build()

                workManager
                    .beginUniqueWork(DOWNLOAD_BUNDLE, ExistingWorkPolicy.REPLACE, workRequest)
                    .enqueue()

                workRequest.id
            } else {
                workInfos[0].id
            }

        workManager
            .getWorkInfoByIdLiveData(workRequestId)
            .observeForever(progressObserver)
    }
}
