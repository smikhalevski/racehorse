package org.racehorse.webview

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import org.greenrobot.eventbus.*
import org.racehorse.OpenInExternalApplicationEvent
import org.racehorse.Plugin

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class AppWebView(private val activity: ComponentActivity) : WebView(activity) {

    private val gson = Gson()
    private val eventBus = EventBus.getDefault()
    private val plugins = ArrayList<Plugin>()

    init {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        webChromeClient = AppWebChromeClient(activity)

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

    fun registerPlugin(plugin: Plugin) {
        plugins.add(plugin)
        plugin.init(activity, eventBus)
        plugin.onRegister()
    }

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
}

internal class AppWebChromeClient(private val activity: ComponentActivity) : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams,
    ): Boolean {

//        if (isPermissionGranted(activity, Manifest.permission.READ_EXTERNAL_STORAGE))



//        fileChooserParams = fileChooserParams
//        filePathCallback = filePathCallback
//        if (Permissions.isGranted(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
//            if (Permissions.isUnknown(activity, Manifest.permission.CAMERA)) {
//                Permissions.requestMissingPermissions(activity, STORAGE_RESULT_CODE, Manifest.permission.CAMERA)
//            } else {
//                openFileDialog()
//            }
//        } else {
//            Permissions.requestMissingPermissions(
//                activity,
//                STORAGE_RESULT_CODE,
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.CAMERA
//            )
//        }
        return true
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
