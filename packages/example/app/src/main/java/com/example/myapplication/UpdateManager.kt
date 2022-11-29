package com.example.myapplication

import android.content.Context
import android.net.Uri
import androidx.work.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit


// - .cache
//     - current
//         - app
//         - descriptor.json
//     - next
//         - app
//         - descriptor.json


// 1




private const val BUNDLE_DESCRIPTOR_URL = "bundleDescriptorUrl"
private const val CURRENT_BUNDLE_VERSION = "currentBundleVersion"

private const val UPDATE_STAGE = "updateStage"
private const val UPDATE_MODE = "updateMode"
private const val DOWNLOAD_PROGRESS = "downloadProgress"
private const val DOWNLOADED_BUNDLE_VERSION = "downloadedBundleVersion"

private const val READING_DESCRIPTOR = "readingDescriptor"
private const val DOWNLOADING_ARCHIVE = "downloadingArchive"
private const val UPDATE_SCHEDULED = "updateScheduled"
private const val UPDATE_COMPLETED = "updateCompleted"



enum class UpdateMode { LAZY, EAGER }

class UpdateManager(private val context: Context, private val eventBus: EventBus) {
    fun load(url: Uri) {
        val workManager = WorkManager.getInstance(context)

        val updateRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    BUNDLE_DESCRIPTOR_URL to "http://example.com",
                    CURRENT_BUNDLE_VERSION to null,
                )
            )
            .build()

        workManager
            .beginUniqueWork("update", ExistingWorkPolicy.KEEP, updateRequest)
            .enqueue()

        workManager
            .getWorkInfoByIdLiveData(updateRequest.id)
            .observeForever { workInfo ->
                val stage = workInfo.progress.getString(UPDATE_STAGE)
            }
    }
}

class UpdateWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val descriptorUrl = inputData.getString(BUNDLE_DESCRIPTOR_URL) ?: return Result.failure()
        val bundleVersion = inputData.getString(CURRENT_BUNDLE_VERSION)

//        applicationContext.filesDir

        setProgressAsync(
            workDataOf(
                UPDATE_STAGE to READING_DESCRIPTOR,
                DOWNLOAD_PROGRESS to 0,
            )
        )

        setProgressAsync(
            workDataOf(
                UPDATE_STAGE to DOWNLOADING_ARCHIVE,
                UPDATE_MODE to UpdateMode.LAZY,
                DOWNLOAD_PROGRESS to 0,
            )
        )

        return Result.success()
    }
}
