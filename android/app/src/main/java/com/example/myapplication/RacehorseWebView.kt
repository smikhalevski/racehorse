package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.greenrobot.eventbus.*

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class RacehorseWebView(
    val appUrl: String,
    context: Context,
    val eventBus: EventBus,
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

        eventBus.register(this)
    }

    fun loadApp() {
        loadUrl("http://$appUrl/index.html")
    }

    fun pushEventToConnectionInbox(ok: Boolean, event: Event) {
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
    fun onSuccessEvent(event: SuccessEvent) {
        pushEventToConnectionInbox(true, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onFailureEvent(event: FailureEvent) {
        pushEventToConnectionInbox(false, event)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onNoSubscriberEvent(event: NoSubscriberEvent) {
        val originalEvent = event.originalEvent

        if (originalEvent is Event) {
            eventBus.post(FailureEvent(originalEvent.requestId, IllegalStateException("No subscriber")))
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSubscriberExceptionEvent(event: SubscriberExceptionEvent) {
        val causingEvent = event.causingEvent

        if (causingEvent is Event) {
            eventBus.post(FailureEvent(causingEvent.requestId, event.throwable))
        }
    }
}

/**
 * Client that serves assets via [assetLoader] if a request comes from the URL with [host].
 */
private class RacehorseWebViewClient(private val host: String, private val assetLoader: WebViewAssetLoader) :
    WebViewClientCompat() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return if (request.url.host == host) assetLoader.shouldInterceptRequest(request.url) else null
    }

    // Support API < 21
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        val uri = Uri.parse(url)

        return if (uri.host == host) assetLoader.shouldInterceptRequest(uri) else null
    }
}

private class RacehorseWebChromeClient : WebChromeClient() {

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams,
    ): Boolean {
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

private class Connection(val gson: Gson, val eventBus: EventBus) {

    @JavascriptInterface
    fun post(requestId: Long, eventStr: String) {
        try {
            val jsonObject = gson.fromJson(eventStr, JsonObject::class.java)
            val type = jsonObject["type"].asString

            jsonObject.remove("type")
            jsonObject.addProperty("requestId", requestId)

            val event = gson.fromJson(jsonObject, Class.forName(type))

            eventBus.post(event)
        } catch (e: Throwable) {
            eventBus.post(FailureEvent(requestId, e))
        }
    }
}

open class Event(val requestId: Long = -1)

open class SuccessEvent(requestId: Long) : Event(requestId)

open class FailureEvent(requestId: Long, cause: Throwable) : Event(requestId) {
    val name = cause::class.java.name
    val message = cause.message
    val stack = cause.stackTraceToString()
}
