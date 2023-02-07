package org.racehorse.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

interface FileChooser {

    fun onShow(
        appWebView: AppWebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean
}
