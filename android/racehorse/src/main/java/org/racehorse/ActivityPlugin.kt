package org.racehorse

import android.app.Activity
import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.SerializableIntent
import org.racehorse.utils.launchActivity
import org.racehorse.utils.launchActivityForResult
import java.io.Serializable

class ActivityInfo(val packageName: String) : Serializable

class GetActivityInfoEvent : RequestEvent() {
    class ResultEvent(val activityInfo: ActivityInfo) : ResponseEvent()
}

/**
 * Starts an activity and doesn't wait for its result.
 *
 * @param intent The intent that starts an activity.
 */
class StartActivityEvent(val intent: SerializableIntent) : RequestEvent() {
    class ResultEvent(val isStarted: Boolean) : ResponseEvent()
}

/**
 * Start an activity for the [intent] and wait for the result.
 */
class StartActivityForResultEvent(val intent: SerializableIntent) : RequestEvent() {
    class ResultEvent(val resultCode: Int, val intent: SerializableIntent?) : ResponseEvent()
}

/**
 * Opens URL in an external app.
 *
 * @param activity The activity that launches the intent to open a URL.
 */
open class ActivityPlugin(private val activity: ComponentActivity) {

    @Subscribe
    open fun onGetActivityInfo(event: GetActivityInfoEvent) {
        event.respond(
            GetActivityInfoEvent.ResultEvent(
                ActivityInfo(packageName = activity.packageName)
            )
        )
    }

    @Subscribe
    open fun onStartActivity(event: StartActivityEvent) {
        event.respond(StartActivityEvent.ResultEvent(activity.launchActivity(event.intent.toIntent())))
    }

    @Subscribe
    open fun onStartActivityForResult(event: StartActivityForResultEvent) {
        val launched = activity.launchActivityForResult(event.intent.toIntent()) {
            event.respond(StartActivityForResultEvent.ResultEvent(it.resultCode, it.data?.let(::SerializableIntent)))
        }

        if (!launched) {
            event.respond(StartActivityForResultEvent.ResultEvent(Activity.RESULT_CANCELED, null))
        }
    }
}
