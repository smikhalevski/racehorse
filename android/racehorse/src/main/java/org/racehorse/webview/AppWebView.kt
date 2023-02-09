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
import org.racehorse.Plugin

@SuppressLint("ViewConstructor")
class AppWebView(
    context: Context,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val gson: Gson = Gson()
) : WebView(context) {

    private val plugins = ArrayList<Plugin>()
    private val cookieManager = CookieManager.getInstance()

    init {
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)

        addJavascriptInterface(Connection(gson, eventBus), "racehorseConnection")

        eventBus.register(this)
    }

    /**
     * Starts the application that is loaded from [appUrl]. Use IP `10.0.2.2` to load content from the host machine of
     * the Android device emulator. [assetLoader] intercepts all requests
     */
    fun start(appUrl: String, assetLoader: WebViewAssetLoader? = null) {
        webChromeClient = AppWebChromeClient()
        webViewClient = AppWebViewClient(appUrl, assetLoader)

        loadUrl(appUrl)

        plugins.forEach(Plugin::onStart)
    }

    /**
     * Persists application data and pauses all plugins.
     */
    fun pause() {
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
        pushEvent(event.requestId, event)
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

        // Support API < 21
        @Suppress("OVERRIDE_DEPRECATION")
        override fun shouldInterceptRequest(view: WebView, requestUrl: String) =
            shouldInterceptRequest(Uri.parse(requestUrl))

        // Support API < 24
        @Suppress("OVERRIDE_DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView, requestUrl: String) =
            shouldOverrideUrlLoading(Uri.parse(requestUrl))
    }
}
