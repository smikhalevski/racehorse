package org.racehorse.webview

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import org.greenrobot.eventbus.*
import org.racehorse.Plugin

/**
 * The [WebView] that manages the web app.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class RacehorseWebView(private val activity: ComponentActivity) : WebView(activity) {

    private val gson = Gson()
    private val eventBus = EventBus.getDefault()

    init {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        webChromeClient = RacehorseWebChromeClient()

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)

        addJavascriptInterface(Connection(gson, eventBus), "racehorseConnection")
    }

    /**
     * Load the app.
     */
    fun start(appUrl: String, pathHandler: WebViewAssetLoader.PathHandler) {
        webViewClient = RacehorseWebViewClient(
            appUrl,
            WebViewAssetLoader.Builder().setHttpAllowed(true).setDomain(appUrl).addPathHandler("/", pathHandler).build()
        )

        loadUrl("http://$appUrl/index.html")
        eventBus.register(this)
    }

    private fun pushEvent(requestId: Int?, event: Any) {
        val json = gson.toJson(gson.toJsonTree(event).asJsonObject.also {
            it.remove("requestId")
            it.addProperty("type", event.javaClass.name)
        })

        evaluateJavascript(
            "(window.racehorseConnection.inbox||(window.racehorseConnection.inbox=[])).push([$requestId,$json])",
            null
        )
    }

    fun registerPlugin(plugin: Plugin) {
        plugin.init(activity, eventBus)
        plugin.start()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onResponseEvent(event: ResponseEvent) {
        pushEvent(event.requestId, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAlertEvent(event: AlertEvent) {
        pushEvent(null, event)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onNoSubscriberEvent(event: NoSubscriberEvent) {
        val originalEvent = event.originalEvent

        if (originalEvent is RequestEvent) {
            eventBus.post(
                ErrorResponseEvent(IllegalStateException("No subscribers for $originalEvent")).setRequestId(
                    originalEvent.requestId
                )
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSubscriberExceptionEvent(event: SubscriberExceptionEvent) {
        when (val causingEvent = event.causingEvent) {

            is ErrorResponseEvent -> causingEvent.cause.printStackTrace()

            is RequestEvent -> eventBus.post(ErrorResponseEvent(event.throwable).setRequestId(causingEvent.requestId))
        }
    }
}
