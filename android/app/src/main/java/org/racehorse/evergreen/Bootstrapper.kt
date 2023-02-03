package org.racehorse.evergreen

import java.io.File
import java.io.IOException
import java.net.URLConnection

/**
 * Manages the app start process, downloads and unzips updates, handles mandatory and background updates, swaps app
 * bundles after restart.
 *
 * @param cacheDir The directory where bootstrapper sores app bundles.
 */
open class Bootstrapper(cacheDir: File) {

    private var masterDir = File(cacheDir, "master")
    private var masterVersionFile = File(cacheDir, "master.version")

    private var updateDir = File(cacheDir, "update")
    private var updateVersionFile = File(cacheDir, "update.version")

    private var updateDownload: BundleDownload? = null

    protected open fun onBundleReady(appDir: File) {}

    protected open fun onUpdateStarted(mandatory: Boolean) {}

    protected open fun onUpdateFailed(mandatory: Boolean, exception: IOException) {}

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
        val masterVersion = masterVersionFile.takeIf { it.exists() }?.readText()
        val updateVersion = updateVersionFile.takeIf { it.exists() }?.readText()

        val masterReady = masterDir.exists()

        if (
            (masterVersion == version && masterReady) ||
            (updateVersion == version && applyUpdate())
        ) {
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
        updateDownload = null

        try {
            updateDir.deleteRecursively()
            updateVersionFile.writeText(version)

            val download = BundleDownload(openConnection(), updateDir, 8192, this::onUpdateProgress)

            updateDownload = download

            download.start()
        } catch (exception: IOException) {
            onUpdateFailed(mandatory, exception)
            return
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
        if (!updateDir.exists()) {
            return false
        }

        masterVersionFile.delete()
        masterDir.deleteRecursively()

        updateDir.renameTo(masterDir)
        updateVersionFile.renameTo(masterVersionFile)
        return true
    }
}
