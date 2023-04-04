package org.racehorse.evergreen

import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.AlertEvent
import java.io.File

/**
 * App assets available in [appDir] and are ready to be used.
 */
class BundleReadyEvent(val appDir: File)

/**
 * The new update download has started.
 */
class UpdateStartedAlertEvent(val mandatory: Boolean) : AlertEvent

/**
 * Failed to download an update.
 */
class UpdateFailedAlertEvent(val mandatory: Boolean, @Transient val cause: Throwable) : AlertEvent

/**
 * A non-mandatory update was successfully downloaded and ready to be applied.
 */
class UpdateReadyAlertEvent : AlertEvent

/**
 * A progress of a pending update download.
 */
class UpdateProgressAlertEvent(val contentLength: Int, val readLength: Long) : AlertEvent

/**
 * The [Bootstrapper] that posts status events to the [eventBus].
 *
 * @param bundlesDir The directory where bootstrapper stores app bundles.
 * @param eventBus The event bus to which status events are posted
 */
class RacehorseBootstrapper(bundlesDir: File, private val eventBus: EventBus) : Bootstrapper(bundlesDir) {

    override fun onBundleReady(appDir: File) {
        eventBus.post(BundleReadyEvent(appDir))
    }

    override fun onUpdateStarted(mandatory: Boolean) {
        eventBus.post(UpdateStartedAlertEvent(mandatory))
    }

    override fun onUpdateFailed(mandatory: Boolean, cause: Throwable) {
        eventBus.post(UpdateFailedAlertEvent(mandatory, cause))
    }

    override fun onUpdateReady() {
        eventBus.post(UpdateReadyAlertEvent())
    }

    override fun onUpdateProgress(contentLength: Int, readLength: Long) {
        eventBus.post(UpdateProgressAlertEvent(contentLength, readLength))
    }
}
