package org.racehorse

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe

@Serializable
class DeviceInfo(
    val apiLevel: Int,
    val brand: String,
    val model: String
)

@Serializable
class Rect(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
)

/**
 * Get OS and device versions.
 */
@Serializable
class GetDeviceInfoEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val info: DeviceInfo) : ResponseEvent()
}

/**
 * Get the list of locales that user picked in the device settings.
 */
@Serializable
class GetPreferredLocalesEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val locales: Set<String>) : ResponseEvent()
}

/**
 * Get the rect that describes the window insets that overlap with system UI.
 *
 * @param typeMask Bit mask of [WindowInsetsCompat.Type]s to query the insets for.
 */
@Serializable
class GetWindowInsetsEvent(val typeMask: Int) : RequestEvent() {

    @Serializable
    class ResultEvent(val rect: Rect) : ResponseEvent()
}

/**
 * Device configuration and general information.
 *
 * @param activity The activity that provides access to window and resources.
 */
open class DevicePlugin(private val activity: ComponentActivity) {

    @Subscribe
    fun onGetDeviceInfo(event: GetDeviceInfoEvent) {
        event.respond(
            GetDeviceInfoEvent.ResultEvent(
                DeviceInfo(apiLevel = Build.VERSION.SDK_INT, brand = Build.BRAND, model = Build.MODEL)
            )
        )
    }

    @Subscribe
    fun onGetPreferredLocales(event: GetPreferredLocalesEvent) {
        val configuration = activity.resources.configuration

        val locales = buildSet<String> {
            for (i in 0 until configuration.locales.size()) {
                add(configuration.locales.get(i).toLanguageTag())
            }
        }

        event.respond(GetPreferredLocalesEvent.ResultEvent(locales))
    }

    @Subscribe
    fun onGetWindowInsets(event: GetWindowInsetsEvent) {
        val density = activity.resources.displayMetrics.density

        val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            ?: return event.respond(GetWindowInsetsEvent.ResultEvent(Rect()))

        val rect = with(windowInsets.getInsets(event.typeMask)) {
            Rect(top / density, right / density, bottom / density, left / density)
        }

        event.respond(GetWindowInsetsEvent.ResultEvent(rect))
    }
}
