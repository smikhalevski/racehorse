package org.racehorse.webview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.net.http.SslError
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import org.greenrobot.eventbus.*

@SuppressLint("ViewConstructor")
class AppWebView(
    context: Context,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val gson: Gson = Gson()
) : WebView(context) {

    companion object {
        const val CONNECTION_KEY = "racehorseConnection"
    }

    /**
     * `true` if the [loadApp] was called, or `false` otherwise.
     */
    var isAppLoaded = false
        private set

    private val plugins = ArrayList<Plugin>()
    private val cookieManager = CookieManager.getInstance()

    init {
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)

        addJavascriptInterface(Connection(gson, eventBus), CONNECTION_KEY)

        eventBus.register(this)
    }

    /**
     * Loads the app from [appUrl].
     *
     * Use IP `10.0.2.2` to load content from the host machine of the Android device emulator.
     */
    fun loadApp(appUrl: String, assetLoader: WebViewAssetLoader? = null) {
        isAppLoaded = true

        webChromeClient = AppWebChromeClient()
        webViewClient = AppWebViewClient(appUrl, assetLoader)

        loadUrl(appUrl)
    }

    /**
     * Starts all plugins.
     */
    fun startPlugins() {
        plugins.forEach(Plugin::onStart)
    }

    /**
     * Persists app data and pauses all plugins.
     */
    fun pausePlugins() {
        cookieManager.flush()
        plugins.forEach(Plugin::onPause)
    }

    /**
     * Initializes the plugin an calls [Plugin.onRegister].
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
     * Publishes the event to the web.
     */
    private fun publishEvent(requestId: Int, event: Any) {
        val json = gson.toJson(gson.toJsonTree(event).asJsonObject.also {
            it.addProperty("type", event::class.java.name)
        })

        evaluateJavascript(
            "(function(conn){conn && conn.inbox && conn.inbox.publish([$requestId, $json])})(window.$CONNECTION_KEY)",
            null
        )
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onResponseEvent(event: ResponseEvent) {
        require(event.requestId >= 0) { "Expected a request ID to be set for a response event" }

        publishEvent(event.requestId, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAlertEvent(event: AlertEvent) {
        publishEvent(-1, event)
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

    /**
     * Calls plugins that implement a given capability. If [callback] returns `true` then the apply is a success,
     * otherwise the next plugin is called.
     */
    private inline fun <reified T> applyPlugin(callback: (T) -> Boolean) = plugins.filterIsInstance<T>().any(callback)

    private inner class AppWebChromeClient : WebChromeClient() {

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            return applyPlugin<FileChooserCapability> {
                it.onShowFileChooser(this@AppWebView, filePathCallback, fileChooserParams)
            }
        }

        override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
            applyPlugin<PermissionsCapability> {
                it.onAskForPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                ) { statuses ->
                    callback.invoke(origin, statuses.containsValue(true), false)
                }
            }
        }
    }

    private inner class AppWebViewClient(appUrl: String, private val assetLoader: WebViewAssetLoader?) :
        WebViewClient() {

        private val appAuthority = Uri.parse(appUrl).authority

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            if (!applyPlugin<HttpsCapability> { it.onReceivedSslError(view, handler, error) }) {
                handler.cancel()
            }
        }

        /**
         * Intercepts any requests, including `fetch` calls.
         */
        private fun shouldInterceptRequest(requestUri: Uri): WebResourceResponse? {
            return if (assetLoader != null && requestUri.authority == appAuthority) {
                assetLoader.shouldInterceptRequest(requestUri)
            } else null
        }

        /**
         * Intercepts `window.location` updates and reloads.
         */
        private fun shouldOverrideUrlLoading(requestUri: Uri): Boolean {
            if (requestUri.authority == appAuthority) {
                return false
            }
            applyPlugin<OpenUrlCapability> { it.onOpenUrl(requestUri) }
            return true
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
            shouldInterceptRequest(request.url)

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
            shouldOverrideUrlLoading(request.url)
    }
}
