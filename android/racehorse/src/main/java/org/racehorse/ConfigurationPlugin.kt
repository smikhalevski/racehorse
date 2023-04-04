package org.racehorse

import android.view.View
import androidx.activity.ComponentActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.*

class GetPreferredLocalesRequestEvent : RequestEvent()

class GetPreferredLocalesResponseEvent(val locales: Array<String>) : ResponseEvent()

class GetWindowInsetsRequestEvent(
    val typeMask: Int =
        WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.navigationBars() or
            WindowInsetsCompat.Type.statusBars()
) : RequestEvent()

class GetWindowInsetsResponseEvent(val rect: Rect) : ResponseEvent()

class KeyboardVisibilityChangedAlertEvent(val isKeyboardVisible: Boolean) : AlertEvent

class Rect(val top: Float, val right: Float, val bottom: Float, val left: Float)

/**
 * Device configuration and general information.
 */
open class ConfigurationPlugin(private val activity: ComponentActivity) : Plugin(), EventBusCapability {

    private var isKeyboardVisible = false

    private val keyboardListener = View.OnApplyWindowInsetsListener { _, windowInsets ->
        with(toWindowInsetsCompat(windowInsets).getInsets(WindowInsetsCompat.Type.ime())) {
            if (isKeyboardVisible != (top + right + bottom + left != 0)) {
                isKeyboardVisible = !isKeyboardVisible
                eventBus.post(KeyboardVisibilityChangedAlertEvent(isKeyboardVisible))
            }
        }
        windowInsets
    }

    override fun onStart() {
        super.onStart()
        activity.window.decorView.setOnApplyWindowInsetsListener(keyboardListener)
    }

    override fun onPause() {
        super.onPause()
        activity.window.decorView.setOnApplyWindowInsetsListener(null)
    }

    @Subscribe
    fun onGetPreferredLocalesRequestEvent(event: GetPreferredLocalesRequestEvent) {
        val configuration = context.resources.configuration

        val locales = ArrayList<String>().apply {
            for (i in 0 until configuration.locales.size()) {
                add(configuration.locales.get(i).toLanguageTag())
            }
        }

        postToChain(event, GetPreferredLocalesResponseEvent(locales.toTypedArray()))
    }

    @Subscribe
    fun onGetWindowInsetsRequestEvent(event: GetWindowInsetsRequestEvent) {
        val density = activity.resources.displayMetrics.density
        val insets = toWindowInsetsCompat(activity.window.decorView.rootWindowInsets).getInsets(event.typeMask)

        val rect = with(insets) {
            Rect(top / density, right / density, bottom / density, left / density)
        }

        postToChain(event, GetWindowInsetsResponseEvent(rect))
    }
}
