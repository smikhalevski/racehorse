package org.racehorse

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.EventBusCapability
import org.racehorse.webview.OpenUrlCapability
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.VoidResponseEvent

/**
 * Opens URL in the external application.
 */
class OpenUrlRequestEvent(val url: String) : RequestEvent()

open class ActionsPlugin(private val activity: ComponentActivity) : Plugin(), EventBusCapability, OpenUrlCapability {

    /**
     * Tries to open [uri] in an external application.
     *
     * @return `true` if an external activity has started, or `false` otherwise.
     */
    fun openUrl(uri: Uri): Boolean {
        val action = if (uri.scheme.equals("tel")) Intent.ACTION_DIAL else Intent.ACTION_VIEW

        val intent = Intent(action, uri).excludePackage(activity.packageManager, arrayOf(activity.packageName))
            ?: return false

        @Suppress("UNREACHABLE_CODE")
        return try {
            ContextCompat.startActivity(activity, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null)
            return true
        } catch (exception: ActivityNotFoundException) {
            return false
        }
    }

    @Subscribe
    fun onOpenUrlRequestEvent(event: OpenUrlRequestEvent) {
        openUrl(Uri.parse(event.url))
        postToChain(event, VoidResponseEvent())
    }
}
