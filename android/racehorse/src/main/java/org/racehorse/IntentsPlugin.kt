package org.racehorse

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.google.gson.annotations.SerializedName
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

enum class SettingsSection {

    @SerializedName("general")
    GENERAL,

    @SerializedName("details")
    DETAILS,

    @SerializedName("development")
    DEVELOPMENT,

    @SerializedName("locale")
    LOCALE,

    @SerializedName("notifications")
    NOTIFICATIONS,
}

/**
 * Starts an activity and doesn't wait for its result.
 *
 * @param intent The intent that starts an activity.
 */
class StartActivityEvent(val intent: WebIntent) : RequestEvent() {
    class ResultEvent(val isStarted: Boolean) : ResponseEvent()
}

/**
 * Start an activity for the [intent] and wait for the result.
 */
class StartActivityForResultEvent(val intent: WebIntent) : RequestEvent() {

    /**
     * @param result The result of an activity, or `null` if there was no activity that can handle the intent.
     */
    class ResultEvent(val result: WebActivityResult<WebIntent?>?) : ResponseEvent()
}

/**
 * Open Settings app and reveal settings of the current application.
 */
class OpenApplicationSettingsEvent(val section: SettingsSection? = null) : WebEvent

/**
 * Opens a URI in an external app.
 *
 * @param uri The URI to open.
 * @param excludedPackageNames The array of package names that shouldn't be used to open the [uri]. If used then the
 * `android.permission.QUERY_ALL_PACKAGES` should be granted, otherwise no activity would be started.
 */
class OpenInExternalApplicationEvent(val uri: String, val excludedPackageNames: Array<String>? = null) :
    RequestEvent() {
    class ResultEvent(val isOpened: Boolean) : ResponseEvent()
}

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
    open fun onStartActivity(event: StartActivityEvent) {
        val started = try {
            ContextCompat.startActivity(activity, event.intent.toIntent(), null)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
        eventBus.postToChain(event, StartActivityEvent.ResultEvent(started))
    }

    @Subscribe
    open fun onStartActivityForResult(event: StartActivityForResultEvent) {
        val started = activity.startActivityForResult(event.intent.toIntent()) {
            eventBus.postToChain(event, StartActivityForResultEvent.ResultEvent(it.toWebActivityResult()))
        }

        if (!started) {
            eventBus.postToChain(event, StartActivityForResultEvent.ResultEvent(null))
        }
    }

    @Subscribe
    open fun onOpenApplicationSettings(event: OpenApplicationSettingsEvent) {
        val intent = when (event.section) {

            SettingsSection.GENERAL -> Intent(Settings.ACTION_APPLICATION_SETTINGS)

            SettingsSection.DETAILS -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + activity.packageName)
            )

            SettingsSection.DEVELOPMENT -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)

            SettingsSection.LOCALE -> Intent(Settings.ACTION_APP_LOCALE_SETTINGS)

            SettingsSection.NOTIFICATIONS ->
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)

            else -> Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
        }

        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
    }

    @Subscribe
    open fun onOpenInExternalApplication(event: OpenInExternalApplicationEvent) {
        eventBus.postToChain(
            event,
            OpenInExternalApplicationEvent.ResultEvent(openInExternalApplication(Uri.parse(event.uri)))
        )
    }

    /**
     * Tries to open [uri] in an external app.
     *
     * @return `true` if an external activity has started, or `false` otherwise.
     */
    protected open fun openInExternalApplication(uri: Uri, excludedPackageNames: Array<String>? = null): Boolean {
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
