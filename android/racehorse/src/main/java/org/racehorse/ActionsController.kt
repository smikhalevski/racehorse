package org.racehorse

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.excludePackages
import org.racehorse.utils.postToChain
import org.racehorse.webview.*

/**
 * Opens URL in the external app.
 */
class OpenUrlRequestEvent(val url: String) : RequestEvent()

class OpenUrlResponseEvent(val isOpened: Boolean) : ResponseEvent()

/**
 * Launches activities for various intents.
 */
open class ActionsController(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    @Subscribe
    open fun onOpenUrl(event: OpenUrlRequestEvent) {
        eventBus.postToChain(event, OpenUrlResponseEvent(openUri(Uri.parse(event.url))))
    }

    /**
     * Tries to open [uri] in an external app.
     *
     * @return `true` if an external activity has started, or `false` otherwise.
     */
    protected open fun openUri(uri: Uri): Boolean {
        val intent =
            Intent(getOpenAction(uri), uri).excludePackages(activity.packageManager, arrayOf(activity.packageName))
                ?: return false

        @Suppress("UNREACHABLE_CODE")
        return try {
            ContextCompat.startActivity(activity, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), null)
            return true
        } catch (exception: ActivityNotFoundException) {
            return false
        }
    }

    protected open fun getOpenAction(uri: Uri): String = when (uri.scheme) {
        // https://developer.android.com/guide/components/intents-common#Phone
        "voicemail",
        "tel" -> Intent.ACTION_DIAL

        // https://developer.android.com/guide/components/intents-common#Messaging
        // https://developer.android.com/guide/components/intents-common#Email
        "sms",
        "smsto",
        "mms",
        "mmsto",
        "mailto" -> Intent.ACTION_SENDTO

        else -> Intent.ACTION_VIEW
    }
}
