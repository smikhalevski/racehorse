package org.racehorse

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

@Serializable
class ProcessStateChangedEvent(val state: Int) : NoticeEvent

@Serializable
class GetProcessStateEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val state: Int) : ResponseEvent()
}

/**
 * Provides info about the current application process.
 *
 * @param eventBus The event bus to which events are posted.
 */
open class ProcessPlugin(private val eventBus: EventBus = EventBus.getDefault()) {

    private companion object {
        const val BACKGROUND = 0
        const val FOREGROUND = 1
        const val ACTIVE = 2
    }

    private val processLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            eventBus.post(ProcessStateChangedEvent(FOREGROUND))
        }

        override fun onPause(owner: LifecycleOwner) {
            eventBus.post(ProcessStateChangedEvent(BACKGROUND))
        }

        override fun onResume(owner: LifecycleOwner) {
            eventBus.post(ProcessStateChangedEvent(ACTIVE))
        }
    }

    open fun enable() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(processLifecycleObserver)
    }

    open fun disable() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(processLifecycleObserver)
    }

    @Subscribe
    open fun onGetProcessState(event: GetProcessStateEvent) {
        event.respond(
            GetProcessStateEvent.ResultEvent(
                when (ProcessLifecycleOwner.get().lifecycle.currentState) {
                    Lifecycle.State.STARTED -> FOREGROUND
                    Lifecycle.State.RESUMED -> ACTIVE
                    else -> BACKGROUND
                }
            )
        )
    }
}
