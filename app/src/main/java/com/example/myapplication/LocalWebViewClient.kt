package com.example.myapplication

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class LocalWebViewClient(private val host: String, private val assetLoader: WebViewAssetLoader) :
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
