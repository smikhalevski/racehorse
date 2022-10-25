package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

val eventBus: EventBus = EventBus.getDefault()

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val webView = configureWebView("foo.com", applicationContext);

        EventBridge(webView, eventBus).register()

//        eventBus.register(this);

        setContentView(webView)
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun configureWebView(host: String, context: Context): WebView {
    val w = WebView(context)

    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)

    with(w) {

        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain(host)
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(context))
            .build()
//        val assetLoader = WebViewAssetLoader.Builder()
//            .setDomain(host)
//            .addPathHandler("/", StaticPathHandler(File("")))
//            .build()

        webViewClient = LocalWebViewClient(host, assetLoader)
        webChromeClient = WebAppChromeClient()

        isHorizontalScrollBarEnabled = false
        isVerticalScrollBarEnabled = false

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setGeolocationEnabled(true)

        loadUrl("https://$host/index.html")
    }
    return w
}

class WebAppChromeClient() : WebChromeClient() {
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