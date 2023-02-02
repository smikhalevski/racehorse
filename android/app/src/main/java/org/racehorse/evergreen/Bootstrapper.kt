package org.racehorse.evergreen

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import androidx.work.WorkInfo.State.*
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Manages the app start process, downloads and unzips updates, handles blocking and background updates, swaps app
 * bundles after restart.
 *
 * @param context The Android app context.
 * @param lifecycleOwner The owner of the update lifecycle.
 * @param updateWorkerClass The worker that would download the web app bundle.
 * @param bundleCacheDir The bootstrapper cache directory.
 */
abstract class Bootstrapper(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val updateWorkerClass: Class<out UpdateWorker>,
    bundleCacheDir: File
) {

    companion object {
        private const val UPDATE_WORK = "update"
    }

    private var activeBundleDir = File(bundleCacheDir, "active")
    private var activeVersionFile = File(bundleCacheDir, "active.version")

    private var updateBundleDir = File(bundleCacheDir, "update")
    private var updateVersionFile = File(bundleCacheDir, "update.version")

    protected abstract suspend fun getUpdateDescriptor(): UpdateDescriptor

    /**
     * Start the app from the [bundleDir].
     *
     * @param bundleDir The directory that contains the app bundle files.
     */
    protected open fun onBundleReady(bundleDir: File) {}

    /**
     * The update download has started.
     *
     * @param blocking `true` if the app should wait until the update is provisioned, `false` otherwise.
     */
    protected open fun onUpdateStarted(blocking: Boolean) {}

    /**
     * The progress the bundle download process.
     */
    protected open fun onUpdateProgress(contentLength: Int, readLength: Long) {}

    /**
     * The non-blocking update was downloaded and ready to be applied.
     */
    protected open fun onUpdateBundleReady() {}

    /**
     * Failed to read the update descriptor.
     *
     * @param bundleDir The directory that contains bundle files, or `null` if there's no active bundle available.
     */
    protected open fun onNoUpdateDescriptor(bundleDir: File?) {}

    /**
     * Checks for updates and triggers [onBundleReady] if the app can be started.
     */
    open fun start() {
        GlobalScope.launch(Dispatchers.IO) {
            checkForUpdateAndStart()
        }
    }

    private suspend fun checkForUpdateAndStart() {
        val updateDescriptor = try {
            getUpdateDescriptor()
        } catch (error: IOException) {
            onNoUpdateDescriptor(if (activeBundleDir.exists()) activeBundleDir else null)
            return
        }

        val activeVersion = if (activeVersionFile.exists()) activeVersionFile.readText() else null
        val updateVersion = if (updateVersionFile.exists()) updateVersionFile.readText() else null

        if (
            (activeVersion == updateDescriptor.version && activeBundleDir.exists()) ||
            (updateVersion == updateDescriptor.version && applyUpdateBundle())
        ) {
            // Active bundle is up-to-date
            onBundleReady(activeBundleDir)
            return
        }

        updateVersionFile.writeText(updateDescriptor.version)
        enqueueUpdateWork(updateDescriptor.url, updateDescriptor.blocking, updateVersion == updateDescriptor.version)
    }

    /**
     * Moves the [updateBundleDir] to [activeBundleDir].
     */
    private fun applyUpdateBundle(): Boolean {
        return updateBundleDir.exists().also {
            activeBundleDir.deleteRecursively()
            activeVersionFile.delete()

            updateBundleDir.renameTo(activeBundleDir)
            updateVersionFile.renameTo(activeVersionFile)
        }
    }

    /**
     * Starts or joins an existing [UPDATE_WORK] and
     */
    private suspend fun enqueueUpdateWork(url: String, blocking: Boolean, joinable: Boolean) {
        val workManager = WorkManager.getInstance(context)

        var workId =
            if (joinable) workManager.getWorkInfosForUniqueWork(UPDATE_WORK).await().firstOrNull()?.id else null

        if (workId == null) {
            val work = OneTimeWorkRequest.Builder(updateWorkerClass)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1000L, TimeUnit.MILLISECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(
                    workDataOf(UpdateWorker.URL to url, UpdateWorker.TARGET_DIR to updateBundleDir.canonicalPath)
                )
                .build()

            workManager.enqueueUniqueWork(UPDATE_WORK, ExistingWorkPolicy.REPLACE, work)
            workId = work.id
        }

        val workData = workManager.getWorkInfoByIdLiveData(workId)

        onUpdateStarted(blocking)

        lateinit var workObserver: Observer<WorkInfo>

        workObserver = Observer<WorkInfo> {
            when (it.state) {
                SUCCEEDED -> {
                    workData.removeObserver(workObserver)

                    if (blocking) {
                        applyUpdateBundle()
                        onBundleReady(activeBundleDir)
                    } else {
                        onUpdateBundleReady()
                    }
                }

                RUNNING -> {
                    onUpdateProgress(
                        it.progress.getInt(UpdateWorker.CONTENT_LENGTH, 0),
                        it.progress.getLong(UpdateWorker.READ_LENGTH, 0L)
                    )
                }

                ENQUEUED, BLOCKED -> {}

                FAILED, CANCELLED -> workData.removeObserver(workObserver)
            }
        }

        withContext(Dispatchers.Main) {
            workData.observe(lifecycleOwner, workObserver)
        }
    }
}
