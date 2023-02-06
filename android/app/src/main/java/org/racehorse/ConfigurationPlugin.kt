package org.racehorse

import android.os.Build
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent

class GetPreferredLocalesRequestEvent : RequestEvent()

class GetPreferredLocalesResponseEvent(val locales: Array<String>) : ResponseEvent()

class ConfigurationPlugin : Plugin() {

    @Subscribe
    fun onGetPreferredLocalesRequestEvent(event: GetPreferredLocalesRequestEvent) {
        val configuration = activity.resources.configuration

        val locales = if (Build.VERSION.SDK_INT < 24) {
            arrayOf(configuration.locale.toLanguageTag())
        } else {
            val locales = ArrayList<String>()
            for (i in 0 until configuration.locales.size()) {
                locales.add(configuration.locales.get(i).toLanguageTag())
            }
            locales.toTypedArray()
        }

        postResponse(event, GetPreferredLocalesResponseEvent(locales))
    }
}
