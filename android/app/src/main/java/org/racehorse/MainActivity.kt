package org.racehorse

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.evergreen.RacehorseBootstrapper
import org.racehorse.evergreen.BundleReadyEvent
import org.racehorse.permissions.PermissionResponder
import org.racehorse.webview.DirectoryPathHandler
import org.racehorse.webview.RacehorseWebView

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var scope: CoroutineScope
    private lateinit var eventBus: EventBus
    private lateinit var bootstrapper: RacehorseBootstrapper
    private lateinit var webView: RacehorseWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scope = CoroutineScope(coroutineContext)
        eventBus = EventBus.getDefault()
        bootstrapper = RacehorseBootstrapper(this, eventBus)
        webView = RacehorseWebView(this, eventBus)

        val permissionResponder = PermissionResponder(this)

        eventBus.register(permissionResponder)
        eventBus.register(this)

//        scope.launch(Dispatchers.IO) {
//            bootstrapper.start("0.0.0", false) {
//                URL("").openConnection()
//            }
//        }

        webView.start("10.0.2.2:1234", WebViewAssetLoader.AssetsPathHandler(this))
        setContentView(webView)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBundleReadyEvent(event: BundleReadyEvent) {
        webView.start("10.0.2.2:1234", DirectoryPathHandler(event.appDir))

        setContentView(webView)
    }
}
