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
 * Opens URL in an external app.
 *
 * @param url The URL to open.
 * @param excludedPackageNames The array of package names that shouldn't be used to open the [url]. If used then the
 * `android.permission.QUERY_ALL_PACKAGES` should be granted, otherwise no activity would be started.
 */
class OpenUrlRequestEvent(val url: String, val excludedPackageNames: Array<String>? = null) : RequestEvent()

class OpenUrlResponseEvent(val isOpened: Boolean) : ResponseEvent()

/**
 * Opens URL in an external app.
 *
 * @param activity The activity that launches the intent to open a URL.
 * @param eventBus The event bus to which events are posted.
 */
open class OpenUrlPlugin(
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
    protected open fun openUri(uri: Uri, excludedPackageNames: Array<String>? = null): Boolean {
        var intent = Intent(getOpenAction(uri), uri)

        if (!excludedPackageNames.isNullOrEmpty()) {
            intent = intent.excludePackages(activity.packageManager, excludedPackageNames) ?: return false
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        @Suppress("UNREACHABLE_CODE")
        return try {
            ContextCompat.startActivity(activity, intent, null)
            return true
        } catch (exception: ActivityNotFoundException) {
            return false
        }
    }

    /**
     * Returns the action name that is most suitable for the [uri].
     */
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
