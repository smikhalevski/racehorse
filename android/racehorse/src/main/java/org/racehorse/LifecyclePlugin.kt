package org.racehorse

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

class GetLifecycleStateEvent : RequestEvent() {
    class ResultEvent(val state: Int) : ResponseEvent()
}

class LifecycleStateChangeEvent(val state: Int) : NoticeEvent

open class LifecyclePlugin(private val eventBus: EventBus = EventBus.getDefault()) {

    companion object {
        const val STARTED = 0
        const val RESUMED = 1
        const val PAUSED = 2
        const val STOPPED = 3
    }

    private val lifecycle = ProcessLifecycleOwner.get().lifecycle

    private val lifecycleListener = LifecycleEventObserver { _, event ->
        val nextState = when (event) {
            Lifecycle.Event.ON_START -> STARTED
            Lifecycle.Event.ON_RESUME -> RESUMED
            Lifecycle.Event.ON_PAUSE -> PAUSED
            Lifecycle.Event.ON_STOP -> STOPPED
            else -> lastState
        }
        if (nextState != lastState) {
            lastState = nextState
            eventBus.post(LifecycleStateChangeEvent(nextState))
        }
    }

    private var lastState = STARTED

    open fun enable() = lifecycle.addObserver(lifecycleListener)

    open fun disable() = lifecycle.removeObserver(lifecycleListener)

    @Subscribe
    open fun onGetLifecycleStateEvent(event: GetLifecycleStateEvent) {
        event.respond(GetLifecycleStateEvent.ResultEvent(lastState))
    }
}
