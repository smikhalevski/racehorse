@file:UseSerializers(FileSerializer::class, ThrowableSerializer::class)

package org.racehorse.evergreen

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.NoticeEvent
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent
import org.racehorse.serializers.FileSerializer
import org.racehorse.serializers.ThrowableSerializer
import java.io.File
import java.net.URLConnection

/**
 * The status of the update bundle.
 *
 * @param version The version of the update.
 * @param isReady `true` if the update is fully downloaded and ready to be applied, or `false` if update is being
 * downloaded.
 */
@Serializable
@Deprecated("Use GetBundleInfoEvent")
class UpdateStatus(val version: String, val isReady: Boolean)

/**
 * App assets available in [appDir] and are ready to be used.
 */
@Serializable
class BundleReadyEvent(val appDir: File) : NoticeEvent

/**
 * The new update download has started.
 */
@Serializable
class UpdateStartedEvent(val updateMode: UpdateMode) : NoticeEvent

/**
 * Failed to download an update.
 */
@Serializable
class UpdateFailedEvent(val updateMode: UpdateMode, val cause: Throwable) : NoticeEvent

/**
 * An update was successfully downloaded and ready to be applied.
 *
 * @param version The version of the update bundle that is ready to be applied.
 */
@Serializable
class UpdateReadyEvent(val version: String) : NoticeEvent

/**
 * Progress of a pending update download.
 *
 * @param contentLength The length of downloaded content in bytes, or -1 if content length cannot be detected.
 * @param readLength The number of bytes that are already downloaded.
 */
@Serializable
class UpdateProgressEvent(val contentLength: Int, val readLength: Long) : NoticeEvent

/**
 * Get the version of the available master bundle.
 */
@Deprecated("Use GetBundleInfoEvent")
class GetMasterVersionEvent : RequestEvent() {

    /**
     * @param version The version of the master bundle or `null` if there's no master bundle.
     */
    class ResultEvent(val version: String?) : ResponseEvent()
}

/**
 * Get the version of the update bundle that would be applied on the next app restart.
 */
@Deprecated("Use GetBundleInfoEvent")
class GetUpdateStatusEvent : RequestEvent() {

    /**
     * @param status The status of the update or `null` if there's no update bundle.
     */
    class ResultEvent(val status: UpdateStatus?) : ResponseEvent()
}

/**
 * Get the info about the current bundle status.
 */
class GetBundleInfoEvent : RequestEvent() {
    class ResultEvent(
        val masterVersion: String?,
        val updateVersion: String?,
        val isMasterReady: Boolean,
        val isUpdateReady: Boolean,
        val masterDir: Uri,
        val updateDir: Uri
    ) : ResponseEvent()
}

/**
 * Starts/restarts the bundle provisioning process.
 *
 * @param version The expected version of the app bundle.
 * @param updateMode The mode of how update is applied.
 * @param openConnection Returns connection that downloads the bundle ZIP archive.
 */
class StartEvent(val version: String, val updateMode: UpdateMode, val openConnection: () -> URLConnection)

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
    @Deprecated("Use GetBundleInfoEvent")
    open fun onGetMasterVersion(event: GetMasterVersionEvent) {
        event.respond(GetMasterVersionEvent.ResultEvent(masterVersion))
    }

    @Subscribe
    @Deprecated("Use GetBundleInfoEvent")
    open fun onGetUpdateStatus(event: GetUpdateStatusEvent) {
        event.respond(GetUpdateStatusEvent.ResultEvent(updateVersion?.let { UpdateStatus(it, isUpdateReady) }))
    }

    @Subscribe
    open fun onGetBundleInfo(event: GetBundleInfoEvent) {
        event.respond(
            GetBundleInfoEvent.ResultEvent(
                masterVersion = masterVersion,
                updateVersion = updateVersion,
                isMasterReady = isMasterReady,
                isUpdateReady = isUpdateReady,
                masterDir = masterDir.toUri(),
                updateDir = updateDir.toUri(),
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onStartEvent(event: StartEvent) {
        start(event.version, event.updateMode, event.openConnection)
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onApplyUpdate(event: ApplyUpdateEvent) {
        event.respond(ApplyUpdateEvent.ResultEvent(if (applyUpdate()) masterVersion else null))
    }
}
