package org.racehorse.evergreen

import android.content.Context
import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.AlertEvent
import java.io.File
import java.io.IOException

class BundleReadyEvent(val appDir: File)

class UpdateFailedEvent(val mandatory: Boolean) : AlertEvent

class UpdateProgressEvent(val contentLength: Int, val readLength: Long) : AlertEvent

class UpdateReadyEvent : AlertEvent

class UpdateStartedEvent(val mandatory: Boolean) : AlertEvent

class RacehorseBootstrapper(context: Context, private val eventBus: EventBus) :
    Bootstrapper(File(context.filesDir, "app")) {

    override fun onBundleReady(appDir: File) {
        eventBus.post(BundleReadyEvent(appDir))
    }

    override fun onUpdateStarted(mandatory: Boolean) {
        eventBus.post(UpdateStartedEvent(mandatory))
    }

    override fun onUpdateFailed(mandatory: Boolean, exception: IOException) {
        eventBus.post(UpdateFailedEvent(mandatory))
    }

    override fun onUpdateReady() {
        eventBus.post(UpdateReadyEvent())
    }

    override fun onUpdateProgress(contentLength: Int, readLength: Long) {
        eventBus.post(UpdateProgressEvent(contentLength, readLength))
    }
}
