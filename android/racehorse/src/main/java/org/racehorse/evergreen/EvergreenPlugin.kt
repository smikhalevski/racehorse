package org.racehorse.evergreen

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.NoticeEvent
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
class UpdateStartedEvent(val mandatory: Boolean) : NoticeEvent

/**
 * Failed to download an update.
 */
class UpdateFailedEvent(val mandatory: Boolean, @Transient val cause: Throwable) : NoticeEvent

/**
 * A non-mandatory update was successfully downloaded and ready to be applied.
 */
class UpdateReadyEvent(val version: String) : NoticeEvent

/**
 * A progress of a pending update download.
 */
class UpdateProgressEvent(val contentLength: Int, val readLength: Long) : NoticeEvent

class GetMasterVersionRequestEvent : RequestEvent()

class GetMasterVersionResponseEvent(val version: String?) : ResponseEvent()

/**
 * Get the version of the update that would be applied on the next app restart.
 */
class GetUpdateVersionRequestEvent : RequestEvent()

class GetUpdateVersionResponseEvent(val version: String?) : ResponseEvent()

/**
 * The [Bootstrapper] that posts status events to the [eventBus].
 *
 * @param bundlesDir The directory where bootstrapper stores app bundles.
 * @param eventBus The event bus to which events are posted.
 */
open class EvergreenPlugin(
    bundlesDir: File,
    private val eventBus: EventBus = EventBus.getDefault()
) : Bootstrapper(bundlesDir) {

    override fun onBundleReady(appDir: File) {
        eventBus.post(BundleReadyEvent(appDir))
    }

    override fun onUpdateStarted(mandatory: Boolean) {
        eventBus.post(UpdateStartedEvent(mandatory))
    }

    override fun onUpdateFailed(mandatory: Boolean, cause: Throwable) {
        eventBus.post(UpdateFailedEvent(mandatory, cause))
    }

    override fun onUpdateReady(version: String) {
        eventBus.post(UpdateReadyEvent(version))
    }

    override fun onUpdateProgress(contentLength: Int, readLength: Long) {
        eventBus.post(UpdateProgressEvent(contentLength, readLength))
    }

    @Subscribe
    open fun onGetMasterVersion(event: GetMasterVersionRequestEvent) {
        eventBus.post(GetMasterVersionResponseEvent(masterVersion))
    }

    @Subscribe
    open fun onGetUpdateVersion(event: GetUpdateVersionRequestEvent) {
        eventBus.post(GetUpdateVersionResponseEvent(updateVersion))
    }
}
