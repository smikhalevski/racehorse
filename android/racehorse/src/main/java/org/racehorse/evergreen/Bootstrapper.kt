package org.racehorse.evergreen

import com.google.gson.annotations.SerializedName
import java.io.File
import java.net.URLConnection

enum class UpdateMode {
    /**
     * If master bundle has a non-matching version, then update bundle is always downloaded and applied before
     * [Bootstrapper.onBundleReady] is called.
     */
    @SerializedName("mandatory")
    MANDATORY,

    /**
     * 1. If there's no master bundle available, then update bundle is downloaded and applied before
     * [Bootstrapper.onBundleReady] is called.
     *
     * 2. If master bundle has a non-matching version, and update bundle with a matching version is downloaded, then
     * update is applied.
     *
     * 3. If master bundle has a non-matching version, and update bundle isn't available, then
     * [Bootstrapper.onBundleReady] is called with master directory and update proceeds in the background. When update
     * is downloaded [Bootstrapper.onUpdateReady] is called.
     */
    @SerializedName("optional")
    OPTIONAL,

    /**
     * 1. If there's no master bundle available, then update bundle is downloaded and applied before
     * [Bootstrapper.onBundleReady] is called.
     *
     * 2. If master bundle is available then [Bootstrapper.onBundleReady] is always called with master directory. Update
     * is downloaded in the background.
     */
    @SerializedName("postponed")
    POSTPONED
}

/**
 * Manages the app start process: downloads and unzips update bundles and handles update resolution.
 *
 * @param bundlesDir The directory where bootstrapper stores app bundles.
 */
open class Bootstrapper(private val bundlesDir: File) {

    val masterVersion get() = masterVersionFile.takeIf { it.exists() }?.readText()
    val updateVersion get() = updateVersionFile.takeIf { it.exists() }?.readText()

    val isMasterReady get() = masterDir.exists()
    val isUpdateReady get() = updateDir.exists() && updateDownload == null

    private var masterDir = File(bundlesDir, "master")
    private var masterVersionFile = File(bundlesDir, "master.version")

    private var updateDir = File(bundlesDir, "update")
    private var updateVersionFile = File(bundlesDir, "update.version")

    private var updateDownload: BundleDownload? = null

    /**
     * App assets available in [appDir] and are ready to be used.
     */
    protected open fun onBundleReady(appDir: File) {}

    /**
     * The new update download has started.
     */
    protected open fun onUpdateStarted(updateMode: UpdateMode) {}

    /**
     * Failed to download an update.
     */
    protected open fun onUpdateFailed(updateMode: UpdateMode, cause: Throwable) {}

    /**
     * An update was successfully downloaded and ready to be applied.
     */
    protected open fun onUpdateReady(version: String) {}

    /**
     * A progress of a pending update download.
     */
    protected open fun onUpdateProgress(contentLength: Int, readLength: Long) {}

    /**
     * Starts/restarts the bundle provisioning process.
     *
     * @param version The expected version of the app bundle.
     * @param updateMode The mode of how update is applied.
     * @param openConnection Returns connection that downloads the bundle ZIP archive.
     */
    fun start(version: String, updateMode: UpdateMode, openConnection: () -> URLConnection) {

        if (isMasterReady && masterVersion == version) {
            updateDownload?.stop()
            updateDownload = null
            onBundleReady(masterDir)
            return
        }

        if (isUpdateReady && updateVersion == version) {
            if (!isMasterReady || updateMode != UpdateMode.POSTPONED) {
                applyUpdate()
            }
            onBundleReady(masterDir)
            return
        }

        if (isMasterReady && updateMode != UpdateMode.MANDATORY) {
            onBundleReady(masterDir)
        }

        onUpdateStarted(updateMode)

        updateDownload?.stop()

        try {
            bundlesDir.mkdirs()

            updateDir.deleteRecursively()
            updateVersionFile.writeText(version)

            val download = BundleDownload(openConnection(), updateDir, 8192, this::onUpdateProgress)

            updateDownload = download

            download.start()
        } catch (throwable: Throwable) {
            throwable.printStackTrace()
            onUpdateFailed(updateMode, throwable)
            return
        } finally {
            updateDownload = null
        }

        if (isMasterReady && updateMode != UpdateMode.MANDATORY) {
            onUpdateReady(version)
        } else {
            applyUpdate()
            onBundleReady(masterDir)
        }
    }

    /**
     * Applies the available update bundle to master.
     */
    fun applyUpdate(): Boolean {
        if (!isUpdateReady) {
            return false
        }

        masterVersionFile.delete()
        masterDir.deleteRecursively()

        updateDir.renameTo(masterDir)
        updateVersionFile.renameTo(masterVersionFile)

        return true
    }
}
