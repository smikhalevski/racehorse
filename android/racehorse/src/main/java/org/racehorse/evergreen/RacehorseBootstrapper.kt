package org.racehorse.evergreen

import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.AlertEvent
import java.io.File

class BundleReadyEvent(val appDir: File)

class UpdateFailedAlertEvent(val mandatory: Boolean) : AlertEvent

class UpdateProgressAlertEvent(val contentLength: Int, val readLength: Long) : AlertEvent

class UpdateReadyAlertEvent : AlertEvent

class UpdateStartedAlertEvent(val mandatory: Boolean) : AlertEvent

class RacehorseBootstrapper(bundlesDir: File, private val eventBus: EventBus) : Bootstrapper(bundlesDir) {

    override fun onBundleReady(appDir: File) {
        eventBus.post(BundleReadyEvent(appDir))
    }

    override fun onUpdateStarted(mandatory: Boolean) {
        eventBus.post(UpdateStartedAlertEvent(mandatory))
    }

    override fun onUpdateFailed(mandatory: Boolean, throwable: Throwable) {
        eventBus.post(UpdateFailedAlertEvent(mandatory))
    }

    override fun onUpdateReady() {
        eventBus.post(UpdateReadyAlertEvent())
    }

    override fun onUpdateProgress(contentLength: Int, readLength: Long) {
        eventBus.post(UpdateProgressAlertEvent(contentLength, readLength))
    }
}
