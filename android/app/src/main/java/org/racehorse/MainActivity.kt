package org.racehorse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.RacehorseWebView

class MainActivity : AppCompatActivity() {

    private val eventBus: EventBus = EventBus.getDefault()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = RacehorseWebView("10.0.2.2:1234", applicationContext, eventBus)

        webView.start()

        setContentView(webView)
    }
}
