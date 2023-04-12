package org.racehorse

import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain

/**
 * Get the list of locales that user picked in the device settings.
 */
class GetPreferredLocalesRequestEvent : RequestEvent()

class GetPreferredLocalesResponseEvent(val locales: Array<String>) : ResponseEvent()

/**
 * Get the rect that describes the window insets that overlap with system UI.
 *
 * @param typeMask Bit mask of [WindowInsetsCompat.Type]s to query the insets for. By default, display cutout,
 * navigation and status bars are included.
 */
class GetWindowInsetsRequestEvent(
    val typeMask: Int =
        WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.navigationBars() or
            WindowInsetsCompat.Type.statusBars()
) : RequestEvent()

class GetWindowInsetsResponseEvent(val rect: Rect) : ResponseEvent()

/**
 * Notifies the web app that the keyboard visibility has changed.
 */
class KeyboardVisibilityChangedEvent(val isKeyboardVisible: Boolean) : NoticeEvent

class Rect(val top: Float, val right: Float, val bottom: Float, val left: Float)

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

    private var isKeyboardVisible = false

    private val keyboardListener = View.OnApplyWindowInsetsListener { _, windowInsets ->
        with(toWindowInsetsCompat(windowInsets).getInsets(WindowInsetsCompat.Type.ime())) {
            if (isKeyboardVisible != (top + right + bottom + left != 0)) {
                isKeyboardVisible = !isKeyboardVisible
                eventBus.post(KeyboardVisibilityChangedEvent(isKeyboardVisible))
            }
        }
        windowInsets
    }

    open fun enable() = activity.window.decorView.setOnApplyWindowInsetsListener(keyboardListener)

    open fun disable() = activity.window.decorView.setOnApplyWindowInsetsListener(null)

    @Subscribe
    fun onGetPreferredLocales(event: GetPreferredLocalesRequestEvent) {
        val configuration = activity.resources.configuration

        val locales = ArrayList<String>().apply {
            for (i in 0 until configuration.locales.size()) {
                add(configuration.locales.get(i).toLanguageTag())
            }
        }

        eventBus.postToChain(event, GetPreferredLocalesResponseEvent(locales.toTypedArray()))
    }

    @Subscribe
    fun onGetWindowInsets(event: GetWindowInsetsRequestEvent) {
        val density = activity.resources.displayMetrics.density
        val insets = toWindowInsetsCompat(activity.window.decorView.rootWindowInsets).getInsets(event.typeMask)

        val rect = with(insets) {
            Rect(top / density, right / density, bottom / density, left / density)
        }

        eventBus.postToChain(event, GetWindowInsetsResponseEvent(rect))
    }
}
