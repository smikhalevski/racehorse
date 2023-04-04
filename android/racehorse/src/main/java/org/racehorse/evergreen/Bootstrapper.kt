package org.racehorse.evergreen

import java.io.File
import java.net.URLConnection

/**
 * Manages the app start process: downloads and unzips update bundles and handles mandatory and background updates.
 *
 * @param bundlesDir The directory where bootstrapper stores app bundles.
 */
open class Bootstrapper(private val bundlesDir: File) {

    val masterVersion get() = masterVersionFile.takeIf { it.exists() }?.readText()
    val updateVersion get() = updateVersionFile.takeIf { it.exists() }?.readText()

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
    protected open fun onUpdateStarted(mandatory: Boolean) {}

    /**
     * Failed to download an update.
     */
    protected open fun onUpdateFailed(mandatory: Boolean, cause: Throwable) {}

    /**
     * A non-mandatory update was successfully downloaded and ready to be applied.
     */
    protected open fun onUpdateReady() {}

    /**
     * A progress of a pending update download.
     */
    protected open fun onUpdateProgress(contentLength: Int, readLength: Long) {}

    /**
     * Starts/restarts the bundle provisioning process.
     *
     * @param version The expected version of the app bundle.
     * @param mandatory If `true` then the app must not start if available bundle version isn't [version], otherwise the
     * app can start if any bundle is available, and update is downloaded in the background.
     * @param openConnection Returns connection that downloads the bundle ZIP archive.
     */
    fun start(
        version: String,
        mandatory: Boolean,
        openConnection: () -> URLConnection
    ) {
        val masterReady = masterDir.exists()

        if ((masterVersion == version && masterReady) || (updateVersion == version && applyUpdate())) {
            updateDownload?.stop()
            updateDownload = null
            onBundleReady(masterDir)
            return
        }

        if (masterReady && !mandatory) {
            onBundleReady(masterDir)
        }

        onUpdateStarted(mandatory)

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
            onUpdateFailed(mandatory, throwable)
            return
        } finally {
            updateDownload = null
        }

        if (masterReady && !mandatory) {
            onUpdateReady()
        } else {
            applyUpdate()
            onBundleReady(masterDir)
        }
    }

    /**
     * Applies update if it is available.
     *
     * @return `true` if update was applied, or `false` if there is no update to apply.
     */
    private fun applyUpdate(): Boolean {
        if (!updateDir.exists() || updateDownload != null) {
            return false
        }

        masterVersionFile.delete()
        masterDir.deleteRecursively()

        updateDir.renameTo(masterDir)
        updateVersionFile.renameTo(masterVersionFile)
        return true
    }
}
