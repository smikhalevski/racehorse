package org.racehorse.evergreen

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.OutboundEvent
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent
import java.io.File

/**
 * App assets available in [appDir] and are ready to be used.
 */
class BundleReadyEvent(val appDir: File)

/**
 * The new update download has started.
 */
class UpdateStartedAlertEvent(val mandatory: Boolean) : OutboundEvent

/**
 * Failed to download an update.
 */
class UpdateFailedAlertEvent(val mandatory: Boolean, @Transient val cause: Throwable) : OutboundEvent

/**
 * A non-mandatory update was successfully downloaded and ready to be applied.
 */
class UpdateReadyAlertEvent(val version: String) : OutboundEvent

/**
 * A progress of a pending update download.
 */
class UpdateProgressAlertEvent(val contentLength: Int, val readLength: Long) : OutboundEvent

class GetUpdateVersionRequestEvent : RequestEvent()

class GetUpdateVersionResponseEvent(val version: String?) : ResponseEvent()

/**
 * The [Bootstrapper] that posts status events to the [eventBus].
 *
 * @param bundlesDir The directory where bootstrapper stores app bundles.
 * @param eventBus The event bus to which status events are posted
 */
open class EvergreenController(
    bundlesDir: File,
    private val eventBus: EventBus = EventBus.getDefault()
) : Bootstrapper(bundlesDir) {

    override fun onBundleReady(appDir: File) {
        eventBus.post(BundleReadyEvent(appDir))
    }

    override fun onUpdateStarted(mandatory: Boolean) {
        eventBus.post(UpdateStartedAlertEvent(mandatory))
    }

    override fun onUpdateFailed(mandatory: Boolean, cause: Throwable) {
        eventBus.post(UpdateFailedAlertEvent(mandatory, cause))
    }

    override fun onUpdateReady(version: String) {
        eventBus.post(UpdateReadyAlertEvent(version))
    }

    override fun onUpdateProgress(contentLength: Int, readLength: Long) {
        eventBus.post(UpdateProgressAlertEvent(contentLength, readLength))
    }

    @Subscribe
    open fun onGetUpdateVersion(event: GetUpdateVersionRequestEvent) {
        eventBus.post(GetUpdateVersionResponseEvent(updateVersion))
    }
}
