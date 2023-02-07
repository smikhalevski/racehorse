package org.racehorse

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import org.racehorse.webview.HttpsCapability

class HttpsPlugin : Plugin(), HttpsCapability {

    override fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError): Boolean {
        // TODO Validate public keys
        handler.proceed()
        return true
    }
}
