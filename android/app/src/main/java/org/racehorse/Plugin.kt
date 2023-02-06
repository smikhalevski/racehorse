package org.racehorse

import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.ChainableEvent
import org.racehorse.webview.setRequestId

open class Plugin {

    val activity get() = _activity

    private lateinit var _activity: ComponentActivity
    private lateinit var _eventBus: EventBus

    fun init(activity: ComponentActivity, eventBus: EventBus) {
        this._activity = activity
        this._eventBus = eventBus
    }

    open fun onCreate() {
        _eventBus.register(this)
    }

    open fun onStart() {}

    open fun onStop() {}

    open fun onDestroy() {
        _eventBus.unregister(this)
    }

    protected fun post(event: Any) {
        _eventBus.post(event)
    }

    protected fun postResponse(causingEvent: ChainableEvent, event: ChainableEvent) {
        post(event.setRequestId(causingEvent.requestId))
    }
}
