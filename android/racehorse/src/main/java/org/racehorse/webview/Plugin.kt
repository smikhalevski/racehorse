package org.racehorse.webview

import android.content.Context
import org.greenrobot.eventbus.EventBus

open class Plugin {

    lateinit var context: Context
    lateinit var eventBus: EventBus

    open fun onRegister() {}

    open fun onStart() {}

    open fun onPause() {}
}
