package org.racehorse

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.SerializableIntent
import org.racehorse.utils.checkActive
import org.racehorse.utils.launchActivity
import org.racehorse.utils.launchActivityForResult
import java.io.Serializable

class ActivityInfo(
    val applicationLabel: String,
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
) : Serializable

class ActivityStateChangedEvent(val state: Int) : NoticeEvent

class GetActivityStateEvent : RequestEvent() {
    class ResultEvent(val state: Int) : ResponseEvent()
}

class GetActivityInfoEvent : RequestEvent() {
    class ResultEvent(val info: ActivityInfo) : ResponseEvent()
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
 * Launches activities for various intents, and provides info about the current activity.
 *
 * @param activity The activity that launches the intent to open a URL.
 * @param eventBus The event bus to which events are posted.
 */
open class ActivityPlugin(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    companion object {
        private const val BACKGROUND = 0
        private const val FOREGROUND = 1
        private const val ACTIVE = 2
    }

    private val lifecycleListener = LifecycleEventObserver { _, event ->
        when (event.targetState) {
            Lifecycle.State.CREATED -> eventBus.post(ActivityStateChangedEvent(BACKGROUND))
            Lifecycle.State.STARTED -> eventBus.post(ActivityStateChangedEvent(FOREGROUND))
            Lifecycle.State.RESUMED -> eventBus.post(ActivityStateChangedEvent(ACTIVE))
            else -> {}
        }
    }

    open fun enable() = activity.lifecycle.addObserver(lifecycleListener)

    open fun disable() = activity.lifecycle.removeObserver(lifecycleListener)

    @Subscribe
    open fun onGetActivityState(event: GetActivityStateEvent) {
        event.respond(
            GetActivityStateEvent.ResultEvent(
                when (activity.lifecycle.currentState) {
                    Lifecycle.State.INITIALIZED, Lifecycle.State.CREATED, Lifecycle.State.DESTROYED -> BACKGROUND
                    Lifecycle.State.STARTED -> FOREGROUND
                    Lifecycle.State.RESUMED -> ACTIVE
                }
            )
        )
    }

    @Subscribe
    open fun onGetActivityInfo(event: GetActivityInfoEvent) {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)

        event.respond(
            GetActivityInfoEvent.ResultEvent(
                ActivityInfo(
                    applicationLabel = activity.applicationInfo.loadLabel(activity.packageManager).toString(),
                    applicationId = activity.packageName,
                    versionName = packageInfo.versionName,
                    versionCode = packageInfo.versionCode,
                )
            )
        )
    }

    @Subscribe
    open fun onStartActivity(event: StartActivityEvent) {
        activity.checkActive()

        event.respond(StartActivityEvent.ResultEvent(activity.launchActivity(event.intent.toIntent())))
    }

    @Subscribe
    open fun onStartActivityForResult(event: StartActivityForResultEvent) {
        activity.checkActive()

        val isLaunched = activity.launchActivityForResult(event.intent.toIntent()) {
            event.respond(StartActivityForResultEvent.ResultEvent(it.resultCode, it.data?.let(::SerializableIntent)))
        }

        if (!isLaunched) {
            event.respond(StartActivityForResultEvent.ResultEvent(Activity.RESULT_CANCELED, null))
        }
    }
}
