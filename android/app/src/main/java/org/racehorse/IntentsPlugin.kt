package org.racehorse

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.VoidResponseEvent
import org.racehorse.webview.respond

// VoidResponseEvent
/**
 * `<queries>` must be present in the manifest file.
 */
class OpenInExternalActivityEvent(val url: String) : RequestEvent()

class IntentsPlugin : Plugin() {

    override fun start() {
        eventBus.register(this)
    }

    override fun stop() {
        eventBus.unregister(this)
    }

    @Subscribe
    fun onOpenInExternalActivity(event: OpenInExternalActivityEvent) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(event.url)).constrainPackage(
            activity.packageManager,
            excludedPackageNames = arrayOf(activity.javaClass.name)
        ) ?: throw IllegalStateException("No external activity can open " + event.url)

        ContextCompat.startActivity(activity, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null)

        event.respond(VoidResponseEvent())
    }
}
