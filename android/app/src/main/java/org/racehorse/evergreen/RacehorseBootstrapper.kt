package org.racehorse.evergreen

import android.content.Context
import org.greenrobot.eventbus.EventBus
import org.racehorse.evergreen.events.*
import java.io.File
import java.io.IOException

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
        eventBus.post(UpdateReady())
    }

    override fun onUpdateProgress(contentLength: Int, readLength: Long) {
        eventBus.post(UpdateProgressEvent(contentLength, readLength))
    }
}
