package org.racehorse

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.connection.Message
import org.racehorse.connection.RacehorseConnection
import org.racehorse.connection.ThreadMode
import org.racehorse.utils.launchActivity
import org.racehorse.utils.launchActivityForResult
import org.racehorse.utils.launchActivityForResultOrNull

@Serializable
class ActivityInfo(
    val applicationLabel: String,
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
)

@Message("org.racehorse.ActivityStateChangedEvent")
@Serializable
class ActivityStateChangedEvent(val state: Int) : RequestEvent()

@Message("org.racehorse.GetActivityStateEvent", threadMode = ThreadMode.SYNC)
@Serializable
class GetActivityStateEvent : RequestEvent() {

    @Message("org.racehorse.GetActivityStateEvent.ResultEvent")
    @Serializable
    class ResultEvent(val state: Int) : ResponseEvent()
}

@Message("org.racehorse.GetActivityInfoEvent", threadMode = ThreadMode.SYNC)
@Serializable
class GetActivityInfoEvent : RequestEvent() {

    @Message("org.racehorse.GetActivityInfoEvent.ResultEvent")
    @Serializable
    class ResultEvent(val info: ActivityInfo) : ResponseEvent()
}

/**
 * Starts an activity and doesn't wait for its result.
 *
 * @param intent The intent that starts an activity.
 */
@Message("org.racehorse.StartActivityEvent", threadMode = ThreadMode.SYNC)
@Serializable
class StartActivityEvent(val intent: @Contextual Intent) : RequestEvent() {

    @Message("org.racehorse.StartActivityEvent.ResultEvent")
    @Serializable
    class ResultEvent(val isStarted: Boolean) : ResponseEvent()
}

/**
 * Start an activity for the [intent] and wait for the result.
 */
@Message("org.racehorse.StartActivityForResultEvent")
@Serializable
class StartActivityForResultEvent(val intent: @Contextual Intent) : RequestEvent() {

    @Message("org.racehorse.StartActivityForResultEvent.ResultEvent")
    @Serializable
    class ResultEvent(val resultCode: Int, val intent: @Contextual Intent?) : ResponseEvent()
}

private const val BACKGROUND = 0
private const val FOREGROUND = 1
private const val ACTIVE = 2

fun RacehorseConnection.activity(activity: ComponentActivity) {

    val lifecycleListener = LifecycleEventObserver { _, event ->
        when (event.targetState) {
            Lifecycle.State.CREATED -> notify(ActivityStateChangedEvent(BACKGROUND))
            Lifecycle.State.STARTED -> notify(ActivityStateChangedEvent(FOREGROUND))
            Lifecycle.State.RESUMED -> notify(ActivityStateChangedEvent(ACTIVE))
            else -> {}
        }
    }

    activity.lifecycle.addObserver(lifecycleListener)

    on<GetActivityStateEvent> {
        GetActivityStateEvent.ResultEvent(
            when (activity.lifecycle.currentState) {
                Lifecycle.State.STARTED -> FOREGROUND
                Lifecycle.State.RESUMED -> ACTIVE
                else -> BACKGROUND
            }
        )
    }

    on<GetActivityInfoEvent> {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)

        @Suppress("DEPRECATION")
        GetActivityInfoEvent.ResultEvent(
            ActivityInfo(
                applicationLabel = activity.applicationInfo.loadLabel(activity.packageManager).toString(),
                applicationId = activity.packageName,
                versionName = packageInfo.versionName ?: "",
                versionCode = packageInfo.versionCode,
            )
        )
    }

    on<StartActivityEvent> {
        StartActivityEvent.ResultEvent(activity.launchActivity(it.intent))
    }

    on<StartActivityForResultEvent> {
        val result = activity.launchActivityForResultOrNull(it.intent)

        if (result == null) {
            StartActivityForResultEvent.ResultEvent(Activity.RESULT_CANCELED, null)
        } else {
            StartActivityForResultEvent.ResultEvent(result.resultCode, result.data)
        }
    }
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

    private companion object {
        const val BACKGROUND = 0
        const val FOREGROUND = 1
        const val ACTIVE = 2
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
                    Lifecycle.State.STARTED -> FOREGROUND
                    Lifecycle.State.RESUMED -> ACTIVE
                    else -> BACKGROUND
                }
            )
        )
    }

    @Subscribe
    open fun onGetActivityInfo(event: GetActivityInfoEvent) {
        val packageInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)

        @Suppress("DEPRECATION")
        event.respond(
            GetActivityInfoEvent.ResultEvent(
                ActivityInfo(
                    applicationLabel = activity.applicationInfo.loadLabel(activity.packageManager).toString(),
                    applicationId = activity.packageName,
                    versionName = packageInfo.versionName ?: "",
                    versionCode = packageInfo.versionCode,
                )
            )
        )
    }

    @Subscribe
    open fun onStartActivity(event: StartActivityEvent) {
        event.respond(StartActivityEvent.ResultEvent(activity.launchActivity(event.intent)))
    }

    @Subscribe
    open fun onStartActivityForResult(event: StartActivityForResultEvent) {
        val isLaunched = activity.launchActivityForResult(event.intent) {
            event.respond(StartActivityForResultEvent.ResultEvent(it.resultCode, it.data))
        }

        if (!isLaunched) {
            event.respond(StartActivityForResultEvent.ResultEvent(Activity.RESULT_CANCELED, null))
        }
    }
}
