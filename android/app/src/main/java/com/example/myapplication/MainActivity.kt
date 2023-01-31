package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val eventBus = EventBus.getDefault()

        val webView = RacehorseWebView("10.0.2.2:1234", applicationContext, eventBus)
//        val webView = AppWebView("foo.com", applicationContext)

//        eventBus.register(this)

        webView.loadApp()

        setContentView(webView)
    }

//    @Subscribe(threadMode = ThreadMode.BACKGROUND)
//    fun onWebReady(event: Any) {
//        print("Ready")
//    }
}
