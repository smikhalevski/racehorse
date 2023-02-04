package org.racehorse.webview

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * Client that serves assets via [assetLoader] if a request comes from the URL with [host].
 */
internal class RacehorseWebViewClient(private val host: String, private val assetLoader: WebViewAssetLoader) :
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
