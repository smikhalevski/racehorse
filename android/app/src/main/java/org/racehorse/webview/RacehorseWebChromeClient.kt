package org.racehorse.webview

import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

internal class RacehorseWebChromeClient : WebChromeClient() {

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
