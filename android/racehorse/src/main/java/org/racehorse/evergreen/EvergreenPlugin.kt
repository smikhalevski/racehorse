package org.racehorse.evergreen

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.NoticeEvent
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent
import org.racehorse.utils.postToChain
import java.io.File

/**
 * App assets available in [appDir] and are ready to be used.
 */
class BundleReadyEvent(val appDir: File) : NoticeEvent

/**
 * The new update download has started.
 */
class UpdateStartedEvent(val updateMode: UpdateMode) : NoticeEvent

/**
 * Failed to download an update.
 */
class UpdateFailedEvent(val updateMode: UpdateMode, @Transient val cause: Throwable) : NoticeEvent

/**
 * An update was successfully downloaded and ready to be applied.
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
class GetUpdateStatusRequestEvent : RequestEvent()

/**
 * @param status The status of the update or `null` if there's no update available.
 */
class GetUpdateStatusResponseEvent(val status: UpdateStatus?) : ResponseEvent()

/**
 * @param version The version of the update.
 * @param isReady `true` if the update is fully downloaded and ready to be applied.
 */
class UpdateStatus(val version: String, val isReady: Boolean)

/**
 * Applies the available update bundle, see [UpdateStatus.isReady].
 */
class ApplyUpdateRequestEvent : RequestEvent()

/**
 * @param version The version of the applied update or `null` if there's no update to apply.
 */
class ApplyUpdateResponseEvent(val version: String?) : ResponseEvent()

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

    override fun onUpdateStarted(updateMode: UpdateMode) {
        eventBus.post(UpdateStartedEvent(updateMode))
    }

    override fun onUpdateFailed(updateMode: UpdateMode, cause: Throwable) {
        eventBus.post(UpdateFailedEvent(updateMode, cause))
    }

    override fun onUpdateReady(version: String) {
        eventBus.post(UpdateReadyEvent(version))
    }

    override fun onUpdateProgress(contentLength: Int, readLength: Long) {
        eventBus.post(UpdateProgressEvent(contentLength, readLength))
    }

    @Subscribe
    open fun onGetMasterVersion(event: GetMasterVersionRequestEvent) {
        eventBus.postToChain(event, GetMasterVersionResponseEvent(masterVersion))
    }

    @Subscribe
    open fun onGetUpdateStatus(event: GetUpdateStatusRequestEvent) {
        eventBus.postToChain(
            event,
            GetUpdateStatusResponseEvent(updateVersion?.let { UpdateStatus(it, isUpdateReady) })
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onApplyUpdate(event: ApplyUpdateRequestEvent) {
        eventBus.postToChain(event, ApplyUpdateResponseEvent(if (applyUpdate()) masterVersion else null))
    }
}
