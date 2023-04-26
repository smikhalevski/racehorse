package org.racehorse

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.WebActivityResult
import org.racehorse.utils.WebIntent
import org.racehorse.utils.excludePackages
import org.racehorse.utils.postToChain
import org.racehorse.utils.startActivityForResult
import org.racehorse.utils.toIntent
import org.racehorse.utils.toWebActivityResult
import org.racehorse.webview.*

/**
 * Starts an activity and doesn't wait for its result.
 *
 * @param intent The intent that starts an activity.
 */
class StartActivityRequestEvent(val intent: WebIntent) : RequestEvent()

/**
 * Response to [StartActivityRequestEvent].
 *
 * @param isStarted `true` if an activity has started, or `false` otherwise.
 */
class StartActivityResponseEvent(val isStarted: Boolean) : ResponseEvent()

/**
 * Start an activity for the [intent] and wait for the result.
 */
class StartActivityForResultRequestEvent(val intent: WebIntent) : RequestEvent()

/**
 * Response to [StartActivityForResultRequestEvent].
 *
 * @param result The result of an activity, or `null` if there was no activity that can handle the intent.
 */
class StartActivityForResultResponseEvent(val result: WebActivityResult<WebIntent?>?) : ResponseEvent()

/**
 * Opens a URI in an external app.
 *
 * @param uri The URI to open.
 * @param excludedPackageNames The array of package names that shouldn't be used to open the [uri]. If used then the
 * `android.permission.QUERY_ALL_PACKAGES` should be granted, otherwise no activity would be started.
 */
class OpenApplicationRequestEvent(val uri: String, val excludedPackageNames: Array<String>? = null) : RequestEvent()

/**
 * Response to [OpenApplicationRequestEvent].
 */
class OpenApplicationResponseEvent(val isOpened: Boolean) : ResponseEvent()

/**
 * Opens URL in an external app.
 *
 * @param activity The activity that launches the intent to open a URL.
 * @param eventBus The event bus to which events are posted.
 */
open class IntentsPlugin(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    @Subscribe
    open fun onStartActivity(event: StartActivityRequestEvent) {
        val started = try {
            ContextCompat.startActivity(activity, event.intent.toIntent(), null)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
        eventBus.postToChain(event, StartActivityResponseEvent(started))
    }

    @Subscribe
    open fun onStartActivityForResult(event: StartActivityForResultRequestEvent) {
        val started = activity.startActivityForResult(event.intent.toIntent()) {
            eventBus.postToChain(event, StartActivityForResultResponseEvent(it.toWebActivityResult()))
        }

        if (!started) {
            eventBus.postToChain(event, StartActivityForResultResponseEvent(null))
        }
    }

    @Subscribe
    open fun onOpenApplication(event: OpenApplicationRequestEvent) {
        eventBus.postToChain(event, OpenApplicationResponseEvent(openApplication(Uri.parse(event.uri))))
    }

    /**
     * Tries to open [uri] in an external app.
     *
     * @return `true` if an external activity has started, or `false` otherwise.
     */
    protected open fun openApplication(uri: Uri, excludedPackageNames: Array<String>? = null): Boolean {
        var intent = Intent(getOpenAction(uri), uri)

        if (!excludedPackageNames.isNullOrEmpty()) {
            intent = intent.excludePackages(activity.packageManager, excludedPackageNames) ?: return false
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        @Suppress("UNREACHABLE_CODE")
        return try {
            ContextCompat.startActivity(activity, intent, null)
            return true
        } catch (_: ActivityNotFoundException) {
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
