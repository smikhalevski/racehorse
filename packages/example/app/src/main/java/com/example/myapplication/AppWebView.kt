package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.webkit.*
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class AppWebView(
    private val host: String,
    context: Context,
    pathHandler: WebViewAssetLoader.PathHandler = WebViewAssetLoader.AssetsPathHandler(context),
) : WebView(context) {

    init {
        with(CookieManager.getInstance()) {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@AppWebView, true)
        }

        webViewClient = AppWebViewClient(host,
            WebViewAssetLoader.Builder().setDomain(host).addPathHandler("/", pathHandler).build())

        webChromeClient = AppWebChromeClient()

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)
    }

    fun loadApp() = loadUrl("https://$host/index.html")
}

/**
 * Client that serves assets via [assetLoader] if a request comes from the [host].
 */
private class AppWebViewClient(private val host: String, private val assetLoader: WebViewAssetLoader) :
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

private class AppWebChromeClient() : WebChromeClient() {
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
