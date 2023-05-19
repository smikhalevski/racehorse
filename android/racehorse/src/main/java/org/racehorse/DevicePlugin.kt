package org.racehorse

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain
import java.io.Serializable

class DeviceInfo(val apiLevel: Int, val brand: String, val model: String) : Serializable

class Rect(val top: Float, val right: Float, val bottom: Float, val left: Float) : Serializable

/**
 * Get OS and device versions.
 */
class GetDeviceInfoEvent : RequestEvent() {
    class ResultEvent(val deviceInfo: DeviceInfo) : ResponseEvent()
}

/**
 * Get the list of locales that user picked in the device settings.
 */
class GetPreferredLocalesEvent : RequestEvent() {
    class ResultEvent(val locales: Array<String>) : ResponseEvent()
}

/**
 * Get the rect that describes the window insets that overlap with system UI.
 *
 * @param typeMask Bit mask of [WindowInsetsCompat.Type]s to query the insets for.
 */
class GetWindowInsetsEvent(val typeMask: Int) : RequestEvent() {
    class ResultEvent(val rect: Rect) : ResponseEvent()
}

/**
 * Device configuration and general information.
 *
 * @param activity The activity that provides access to window and resources.
 * @param eventBus The event bus to which events are posted.
 */
open class DevicePlugin(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    @Subscribe
    fun onGetDeviceInfo(event: GetDeviceInfoEvent) {
        eventBus.postToChain(
            event,
            GetDeviceInfoEvent.ResultEvent(
                DeviceInfo(apiLevel = Build.VERSION.SDK_INT, brand = Build.BRAND, model = Build.MODEL)
            )
        )
    }

    @Subscribe
    fun onGetPreferredLocales(event: GetPreferredLocalesEvent) {
        val configuration = activity.resources.configuration

        val locales = ArrayList<String>().apply {
            for (i in 0 until configuration.locales.size()) {
                add(configuration.locales.get(i).toLanguageTag())
            }
        }

        eventBus.postToChain(event, GetPreferredLocalesEvent.ResultEvent(locales.toTypedArray()))
    }

    @Subscribe
    fun onGetWindowInsets(event: GetWindowInsetsEvent) {
        val density = activity.resources.displayMetrics.density
        val insets = toWindowInsetsCompat(activity.window.decorView.rootWindowInsets).getInsets(event.typeMask)

        val rect = with(insets) {
            Rect(top / density, right / density, bottom / density, left / density)
        }

        eventBus.postToChain(event, GetWindowInsetsEvent.ResultEvent(rect))
    }
}
