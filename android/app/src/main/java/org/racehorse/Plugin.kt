package org.racehorse

import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.EventBus

open class Plugin {

    val activity get() = _activity

    private lateinit var _activity: ComponentActivity
    private lateinit var _eventBus: EventBus

    fun init(activity: ComponentActivity, eventBus: EventBus) {
        this._activity = activity
        this._eventBus = eventBus
    }

    open fun onStart() {
        _eventBus.register(this)
    }

    open fun onStop() {
        _eventBus.unregister(this)
    }

    protected fun post(event: Any) {
        _eventBus.post(event)
    }
}
