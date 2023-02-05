package org.racehorse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import org.racehorse.webview.RacehorseWebView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = RacehorseWebView(this)

        webView.registerPlugin(PermissionPlugin())
        webView.registerPlugin(NetworkPlugin())

        webView.start("10.0.2.2:1234", WebViewAssetLoader.AssetsPathHandler(this))

        setContentView(webView)
    }
}
