package org.racehorse.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import org.greenrobot.eventbus.*
import org.racehorse.OpenInExternalApplicationEvent
import org.racehorse.Plugin

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class AppWebView(context: Context) : WebView(context) {

    private val gson = Gson()
    private val eventBus = EventBus.getDefault()
    private val plugins = ArrayList<Plugin>()

    init {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        webChromeClient = AppWebChromeClient()

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)

        addJavascriptInterface(Connection(gson, eventBus), "racehorseConnection")

        eventBus.register(this)
    }

    fun start(appUrl: String, assetLoader: WebViewAssetLoader? = null) {
        if (assetLoader != null) {
            webViewClient = AppWebViewClient(appUrl, assetLoader, eventBus)
        }

        loadUrl(appUrl)

        plugins.forEach(Plugin::onStart)
    }

    fun pause() {
        CookieManager.getInstance().flush()
        plugins.forEach(Plugin::onPause)
    }

    /**
     * Initializes plugin an calls [Plugin.onRegister].
     *
     * Plugins with [EventBusCapability] are also registered in the [eventBus].
     */
    fun registerPlugin(plugin: Plugin): AppWebView {
        if (plugins.contains(plugin)) {
            return this
        }

        plugin.context = context
        plugin.eventBus = eventBus

        if (plugin is EventBusCapability) {
            eventBus.register(plugin)
        }

        plugin.onRegister()

        plugins.add(plugin)
        return this
    }

    /**
     * Pushes the event to the web.
     */
    private fun pushEvent(requestId: Int?, event: Any) {
        val json = gson.toJson(gson.toJsonTree(event).asJsonObject.also {
            it.remove("requestId")
            it.addProperty("type", event::class.java.name)
        })

        evaluateJavascript(
            "(window.racehorseConnection.inbox||(window.racehorseConnection.inbox=[])).push([$requestId,$json])",
            null
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onResponseEvent(event: ResponseEvent) {
        if (event.requestId != -1) {
            pushEvent(event.requestId, event)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAlertEvent(event: AlertEvent) {
        pushEvent(null, event)
    }

    @Subscribe
    fun onNoSubscriberEvent(event: NoSubscriberEvent) {
        val originalEvent = event.originalEvent

        if (originalEvent is RequestEvent) {
            eventBus.post(
                ExceptionResponseEvent(IllegalStateException("No subscribers for $originalEvent"))
                    .setRequestId(originalEvent.requestId)
            )
        }
    }

    @Subscribe
    fun onSubscriberExceptionEvent(event: SubscriberExceptionEvent) {
        when (val causingEvent = event.causingEvent) {

            is ExceptionResponseEvent -> causingEvent.cause.printStackTrace()

            is RequestEvent -> eventBus.post(ExceptionResponseEvent(event.throwable).setRequestId(causingEvent.requestId))
        }
    }

    inner class AppWebChromeClient : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            return plugins.filterIsInstance<FileChooserCapability>().any {
                it.onShowFileChooser(this@AppWebView, filePathCallback, fileChooserParams)
            }
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            plugins.filterIsInstance<PermissionsCapability>().any {
                it.askForPermission(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
                    callback.invoke(origin, granted, false)
                }
            }
        }
    }
}

internal class AppWebViewClient(
    private val appUrl: String,
    private val assetLoader: WebViewAssetLoader,
    private val eventBus: EventBus,
) : WebViewClient() {

    private val appUri = Uri.parse(appUrl)

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return if (request.url.authority == appUri.authority) assetLoader.shouldInterceptRequest(request.url) else null
    }

    // Support API < 21
    override fun shouldInterceptRequest(view: WebView, requestUrl: String): WebResourceResponse? {
        val requestUri = Uri.parse(requestUrl)

        return if (requestUri.authority == appUri.authority) assetLoader.shouldInterceptRequest(requestUri) else null
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.authority === appUri.authority) {
            return false
        }
        eventBus.post(OpenInExternalApplicationEvent(request.url.toString()))
        return true
    }

    // Support API < 24
    override fun shouldOverrideUrlLoading(view: WebView, requestUrl: String): Boolean {
        val requestUri = Uri.parse(requestUrl)

        if (requestUri.authority === appUri.authority) {
            return false
        }
        eventBus.post(OpenInExternalApplicationEvent(requestUrl))
        return true
    }
}
