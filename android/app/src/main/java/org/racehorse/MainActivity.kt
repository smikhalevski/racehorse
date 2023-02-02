package org.racehorse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.RacehorseWebView

class MainActivity : AppCompatActivity() {

    private val eventBus: EventBus = EventBus.getDefault()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = RacehorseWebView(applicationContext, eventBus)

//        RacehorseUpdateManager(webView, eventBus, "https://example.com/version.json").update()

        webView.start("10.0.2.2:1234", WebViewAssetLoader.AssetsPathHandler(applicationContext))

        setContentView(webView)
    }
}
