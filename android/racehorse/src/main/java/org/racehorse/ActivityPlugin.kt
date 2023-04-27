package org.racehorse

import android.app.Activity
import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.WebIntent
import org.racehorse.utils.launchActivity
import org.racehorse.utils.launchActivityForResult
import org.racehorse.utils.postToChain

class ActivityInfo(val packageName: String)

class GetActivityInfoEvent : RequestEvent() {
    class ResultEvent(val activityInfo: ActivityInfo) : ResponseEvent()
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
    class ResultEvent(val resultCode: Int, val intent: WebIntent?) : ResponseEvent()
}

/**
 * Opens URL in an external app.
 *
 * @param activity The activity that launches the intent to open a URL.
 * @param eventBus The event bus to which events are posted.
 */
open class ActivityPlugin(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    @Subscribe
    open fun onGetActivityInfo(event: GetActivityInfoEvent) {
        eventBus.postToChain(
            event,
            GetActivityInfoEvent.ResultEvent(
                ActivityInfo(packageName = activity.packageName)
            )
        )
    }

    @Subscribe
    open fun onStartActivity(event: StartActivityEvent) {
        eventBus.postToChain(event, StartActivityEvent.ResultEvent(activity.launchActivity(event.intent.toIntent())))
    }

    @Subscribe
    open fun onStartActivityForResult(event: StartActivityForResultEvent) {
        val launched = activity.launchActivityForResult(event.intent.toIntent()) {
            eventBus.postToChain(
                event,
                StartActivityForResultEvent.ResultEvent(it.resultCode, it.data?.let(::WebIntent))
            )
        }

        if (!launched) {
            eventBus.postToChain(event, StartActivityForResultEvent.ResultEvent(Activity.RESULT_CANCELED, null))
        }
    }
}
