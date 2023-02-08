package org.racehorse.webview

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView

/**
 * Plugins with this capability can resolve SSL errors.
 */
interface HttpsCapability {

    fun onReceivedSslError(webView: WebView, handler: SslErrorHandler, error: SslError): Boolean {
        return false
    }
}
