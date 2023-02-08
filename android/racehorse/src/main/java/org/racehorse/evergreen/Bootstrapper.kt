package org.racehorse.evergreen

import java.io.File
import java.net.URLConnection

/**
 * Manages the app start process, downloads and unzips updates, handles mandatory and background updates, swaps app
 * bundles after restart.
 *
 * @param bundlesDir The directory where bootstrapper sores app bundles.
 */
open class Bootstrapper(private val bundlesDir: File) {

    val masterVersion get() = masterVersionFile.takeIf { it.exists() }?.readText()
    val updateVersion get() = updateVersionFile.takeIf { it.exists() }?.readText()

    private var masterDir = File(bundlesDir, "master")
    private var masterVersionFile = File(bundlesDir, "master.version")

    private var updateDir = File(bundlesDir, "update")
    private var updateVersionFile = File(bundlesDir, "update.version")

    private var updateDownload: BundleDownload? = null

    protected open fun onBundleReady(appDir: File) {}

    protected open fun onUpdateStarted(mandatory: Boolean) {}

    protected open fun onUpdateFailed(mandatory: Boolean, throwable: Throwable) {}

    protected open fun onUpdateReady() {}

    protected open fun onUpdateProgress(contentLength: Int, readLength: Long) {}

    /**
     * Starts/restarts the bundle provisioning process.
     *
     * @param version The expected version of the app bundle.
     * @param mandatory If `true` then the app must not start if version isn't [version], otherwise the app can start
     * if any bundle is available, and update is downloaded in the background.
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
     * Applies update it it is available.
     *
     * @return `true` is update was applied, or `false` if there is no update to apply.
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
