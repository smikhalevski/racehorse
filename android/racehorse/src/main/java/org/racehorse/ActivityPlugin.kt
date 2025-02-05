@file:UseSerializers(IntentSerializer::class)

package org.racehorse

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.serializers.IntentSerializer
import org.racehorse.utils.launchActivity
import org.racehorse.utils.launchActivityForResult

@Serializable
class ActivityInfo(
    val applicationLabel: String,
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
)

@Serializable
class ActivityStateChangedEvent(val state: Int) : NoticeEvent

@Serializable
class GetActivityStateEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val state: Int) : ResponseEvent()
}

@Serializable
class GetActivityInfoEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val info: ActivityInfo) : ResponseEvent()
}

/**
 * Starts an activity and doesn't wait for its result.
 *
 * @param intent The intent that starts an activity.
 */
@Serializable
class StartActivityEvent(val intent: Intent) : RequestEvent() {

    @Serializable
    class ResultEvent(val isStarted: Boolean) : ResponseEvent()
}

/**
 * Start an activity for the [intent] and wait for the result.
 */
@Serializable
class StartActivityForResultEvent(val intent: Intent) : RequestEvent() {

    @Serializable
    class ResultEvent(val resultCode: Int, val intent: Intent?) : ResponseEvent()
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
