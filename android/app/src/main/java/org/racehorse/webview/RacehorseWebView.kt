package org.racehorse.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import org.greenrobot.eventbus.*
import org.racehorse.events.Event
import org.racehorse.events.ErrEvent
import org.racehorse.events.OkEvent

/**
 * The [WebView] that manages the web app.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class RacehorseWebView(
    private val appUrl: String,
    context: Context,
    private val eventBus: EventBus,
    pathHandler: WebViewAssetLoader.PathHandler = WebViewAssetLoader.AssetsPathHandler(context),
) : WebView(context) {

    private val gson = Gson()

    init {
        with(CookieManager.getInstance()) {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@RacehorseWebView, true)
        }

        webViewClient = RacehorseWebViewClient(
            appUrl,
            WebViewAssetLoader.Builder().setHttpAllowed(true).setDomain(appUrl).addPathHandler("/", pathHandler).build()
        )

        webChromeClient = RacehorseWebChromeClient()

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)

        addJavascriptInterface(Connection(gson, eventBus), "racehorseConnection")
    }

    /**
     * Load the app.
     */
    fun start() {
        loadUrl("http://$appUrl/index.html")
        eventBus.register(this)
    }

    private fun pushToInbox(ok: Boolean, event: Event) {
        val jsonObject = gson.toJsonTree(event).asJsonObject
        jsonObject.remove("requestId")
        jsonObject.addProperty("type", event.javaClass.name)

        val eventStr = gson.toJson(jsonObject)

        evaluateJavascript(
            "(window.racehorseConnection.inbox||(window.racehorseConnection.inbox=[]))" +
                    ".push({requestId:${event.requestId},ok:${ok},event:${eventStr}})",
            null
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOkEvent(event: OkEvent) {
        pushToInbox(true, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onErrEvent(event: ErrEvent) {
        pushToInbox(false, event)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onNoSubscriberEvent(event: NoSubscriberEvent) {
        val originalEvent = event.originalEvent

        if (originalEvent is Event) {
            eventBus.post(ErrEvent(originalEvent.requestId, IllegalStateException("No subscriber")))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSubscriberExceptionEvent(event: SubscriberExceptionEvent) {
        val causingEvent = event.causingEvent

        if (causingEvent is Event) {
            eventBus.post(ErrEvent(causingEvent.requestId, event.throwable))
        }
    }
}
