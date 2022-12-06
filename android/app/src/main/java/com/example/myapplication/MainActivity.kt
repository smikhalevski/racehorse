package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus

val eventBus: EventBus = EventBus.getDefault()

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = AppWebView(BuildConfig.DEV_SERVER_URL, applicationContext)
//        val webView = AppWebView("foo.com", applicationContext)

        EventBridge(webView, eventBus).register()

        webView.loadApp()

//        eventBus.register(this)

        setContentView(webView)
    }
}
