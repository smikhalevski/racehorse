package org.racehorse

import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.EventBus

open class Plugin {

    protected lateinit var activity: ComponentActivity
    protected lateinit var eventBus: EventBus

    fun init(activity: ComponentActivity, eventBus: EventBus) {
        this.activity = activity
        this.eventBus = eventBus
    }

    open fun start() {}

    open fun stop() {}
}
