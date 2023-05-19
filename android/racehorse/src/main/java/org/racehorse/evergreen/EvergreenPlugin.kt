package org.racehorse.evergreen

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.NoticeEvent
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent
import org.racehorse.utils.postToChain
import java.io.File
import java.io.Serializable

/**
 * The status of the update bundle.
 *
 * @param version The version of the update.
 * @param isReady `true` if the update is fully downloaded and ready to be applied, or `false` if update is being
 * downloaded.
 */
class UpdateStatus(val version: String, val isReady: Boolean) : Serializable

/**
 * App assets available in [appDir] and are ready to be used.
 */
class BundleReadyEvent(@Transient val appDir: File) : NoticeEvent

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
 *
 * @param version The version of the update bundle that is ready to be applied.
 */
class UpdateReadyEvent(val version: String) : NoticeEvent

/**
 * A progress of a pending update download.
 *
 * @param contentLength The length of downloaded content in bytes, or -1 if content length cannot be detected.
 * @param readLength The number of bytes that are already downloaded.
 */
class UpdateProgressEvent(val contentLength: Int, val readLength: Long) : NoticeEvent

/**
 * Get the version of the available master bundle.
 */
class GetMasterVersionEvent : RequestEvent() {

    /**
     * @param version The version of the master bundle or `null` if there's no master bundle.
     */
    class ResultEvent(val version: String?) : ResponseEvent()
}

/**
 * Get the version of the update bundle that would be applied on the next app restart.
 */
class GetUpdateStatusEvent : RequestEvent() {

    /**
     * @param status The status of the update or `null` if there's no update bundle.
     */
    class ResultEvent(val status: UpdateStatus?) : ResponseEvent()
}

/**
 * Applies the available update bundle to master, see [UpdateStatus.isReady].
 */
class ApplyUpdateEvent : RequestEvent() {

    /**
     * @param version The version of the applied update or `null` if there's no update to apply.
     */
    class ResultEvent(val version: String?) : ResponseEvent()
}

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
    open fun onGetMasterVersion(event: GetMasterVersionEvent) {
        eventBus.postToChain(event, GetMasterVersionEvent.ResultEvent(masterVersion))
    }

    @Subscribe
    open fun onGetUpdateStatus(event: GetUpdateStatusEvent) {
        eventBus.postToChain(
            event,
            GetUpdateStatusEvent.ResultEvent(updateVersion?.let { UpdateStatus(it, isUpdateReady) })
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onApplyUpdate(event: ApplyUpdateEvent) {
        eventBus.postToChain(event, ApplyUpdateEvent.ResultEvent(if (applyUpdate()) masterVersion else null))
    }
}
