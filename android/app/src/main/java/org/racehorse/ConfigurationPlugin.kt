package org.racehorse

import android.content.res.Configuration
import android.os.Build
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent
import org.racehorse.webview.respond

class GetPreferredLocalesRequestEvent : RequestEvent()

class GetPreferredLocalesResponseEvent(val locales: Array<String>) : ResponseEvent()

class ConfigurationPlugin : Plugin() {

    private lateinit var configuration: Configuration

    override fun start() {
        eventBus.register(this)
        configuration = activity.resources.configuration
    }

    override fun stop() {
        eventBus.unregister(this)
    }

    @Subscribe
    fun onGetPreferredLocalesRequestEvent(event: GetPreferredLocalesRequestEvent) {
        val locales = if (Build.VERSION.SDK_INT < 24) {
            arrayOf(configuration.locale.toLanguageTag())
        } else {
            val locales = ArrayList<String>()
            for (i in 0 until configuration.locales.size()) {
                locales.add(configuration.locales.get(i).toLanguageTag())
            }
            locales.toTypedArray()
        }

        event.respond(GetPreferredLocalesResponseEvent(locales))
    }
}
