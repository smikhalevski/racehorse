package org.racehorse

import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.VoidResponseEvent

// VoidResponseEvent
/**
 * `<queries>` must be present in the manifest file.
 */
class OpenInExternalApplicationEvent(val url: String) : RequestEvent()

class IntentsPlugin : Plugin() {

    @Subscribe
    fun onOpenInExternalApplicationEvent(event: OpenInExternalApplicationEvent) {
        val uri = Uri.parse(event.url)
        val action = if (uri.scheme.equals("tel")) Intent.ACTION_DIAL else Intent.ACTION_VIEW

        val intent = Intent(action, uri).excludePackage(activity.packageManager, arrayOf(activity.packageName))
            ?: throw IllegalStateException("Cannot open $action $uri in any external application")

        ContextCompat.startActivity(activity, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null)

        postResponse(event, VoidResponseEvent())
    }
}
