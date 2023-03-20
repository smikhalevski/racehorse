package org.racehorse

import android.annotation.SuppressLint
import android.os.Build
import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.EventBusCapability
import org.racehorse.webview.Plugin
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent

class GetPreferredLocalesRequestEvent : RequestEvent()

class GetPreferredLocalesResponseEvent(val locales: Array<String>) : ResponseEvent()

class GetWindowInsetsRequestEvent : RequestEvent()

class GetWindowInsetsResponseEvent(val rect: Rect) : ResponseEvent()

class Rect(val top: Int, val right: Int, val bottom: Int, val left: Int)

/**
 * Device configuration and general information.
 */
class ConfigurationPlugin(private val activity: ComponentActivity) : Plugin(), EventBusCapability {

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
        val rect = try {
            getWindowInsets()
        } catch (throwable: Throwable) {
            Rect(0, 0, 0, 0)
        }

        postToChain(event, GetWindowInsetsResponseEvent(rect))
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    fun getWindowInsets(): Rect = with(activity.resources) {
        val density = displayMetrics.density

        var statusBarHeight = 0f
        var navigationBarHeight = 0f

        if (Build.VERSION.SDK_INT >= 28) {
            activity.window.decorView.rootWindowInsets.displayCutout?.let {
                statusBarHeight = it.safeInsetTop / density
                navigationBarHeight = it.safeInsetBottom / density
            }
        }

        if (statusBarHeight == 0f) {
            statusBarHeight = getDimensionPixelSize(
                getIdentifier("status_bar_height", "dimen", "android")
            ) / density
        }

        if (navigationBarHeight == 0f) {
            navigationBarHeight = getDimensionPixelSize(
                getIdentifier("navigation_bar_height", "dimen", "android")
            ) / density
        }

        Rect(statusBarHeight.toInt(), 0, navigationBarHeight.toInt(), 0)
    }
}
