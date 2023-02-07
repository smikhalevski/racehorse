package org.racehorse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.racehorse.webview.AppWebView

class MainActivity : AppCompatActivity() {

    lateinit var webView: AppWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = AppWebView(this)
            .registerPlugin(HttpsPlugin())
            .registerPlugin(PermissionsPlugin(this))
            .registerPlugin(NetworkPlugin())
            .registerPlugin(ConfigurationPlugin())
            .registerPlugin(IntentsPlugin(this))
            .registerPlugin(GooglePlayReferrerPlugin())
            .registerPlugin(FileChooserPlugin(this, externalCacheDir, "$packageName.provider"))

        // 1️⃣ Debug in emulator with server on localhost:1234
        webView.start("https://10.0.2.2:1234")

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
