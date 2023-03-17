package org.racehorse

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.*

/**
 * Opens URL in the external application.
 */
class OpenUrlRequestEvent(val url: String) : RequestEvent()

class OpenUrlResponseEvent(val opened: Boolean) : ResponseEvent()

/**
 * Launches activities for various intents.
 */
open class ActionsPlugin(private val activity: ComponentActivity) : Plugin(), EventBusCapability, OpenUrlCapability {

    /**
     * Tries to open [uri] in an external application.
     *
     * @return `true` if an external activity has started, or `false` otherwise.
     */
    override fun onOpenUrl(uri: Uri): Boolean {
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
        postToChain(event, OpenUrlResponseEvent(onOpenUrl(Uri.parse(event.url))))
    }
}
