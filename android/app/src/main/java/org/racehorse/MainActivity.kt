package org.racehorse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import org.racehorse.webview.AppWebView

class MainActivity : AppCompatActivity() {

    lateinit var webView: AppWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = AppWebView(this)

        webView.registerPlugin(PermissionsPlugin())
        webView.registerPlugin(NetworkPlugin())
        webView.registerPlugin(ConfigurationPlugin())
        webView.registerPlugin(IntentsPlugin())
        webView.registerPlugin(GooglePlayReferrerPlugin())

        // 1️⃣ Debug in emulator with server on localhost:1234
        webView.start("http://10.0.2.2:1234")

//        // 2️⃣ Load app bundle from src/main/assets folder
//        webView.start(
//            "http://example.com",
//            WebViewAssetLoader.Builder()
//                .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
//                .build()
//        )

        setContentView(webView)
    }

    override fun onPause() {
        super.onPause()
        webView.pause()
    }
}
