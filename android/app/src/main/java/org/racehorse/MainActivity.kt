package org.racehorse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import org.racehorse.webview.RacehorseWebView

class MainActivity : AppCompatActivity() {

    lateinit var webView: RacehorseWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = RacehorseWebView(this)

        webView.registerPlugin(PermissionsPlugin())
        webView.registerPlugin(NetworkPlugin())
        webView.registerPlugin(ConfigurationPlugin())
        webView.registerPlugin(IntentsPlugin())
        webView.registerPlugin(GooglePlayReferrerPlugin())

        webView.start("10.0.2.2:1234", WebViewAssetLoader.AssetsPathHandler(this))

        setContentView(webView)
    }

}
