package org.racehorse

import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.ChainableEvent
import org.racehorse.webview.setRequestId

open class Plugin {

    val activity get() = _activity

    private lateinit var _activity: ComponentActivity
    private lateinit var _eventBus: EventBus

    open fun onRegister(activity: ComponentActivity, eventBus: EventBus) {
        _activity = activity
        _eventBus = eventBus

        _eventBus.register(this)
    }

    open fun onStart() {}

    open fun onPause() {}

    protected fun post(event: Any) {
        _eventBus.post(event)
    }

    protected fun postResponse(causingEvent: ChainableEvent, event: ChainableEvent) {
        post(event.setRequestId(causingEvent.requestId))
    }
}
