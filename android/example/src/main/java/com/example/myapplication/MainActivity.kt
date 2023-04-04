package com.example.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.*
import org.racehorse.evergreen.BundleReadyEvent
import org.racehorse.webview.AppWebView
import org.racehorse.webview.StaticPathHandler

class MainActivity : AppCompatActivity() {

    private val eventBus = EventBus.getDefault()

    private val networkMonitor = NetworkMonitor(eventBus, this)

    private val webView: AppWebView by lazy {
        AppWebView(this, eventBus)
            .registerPlugin(HttpsPlugin())
            .registerPlugin(PermissionsPlugin(this))
            .registerPlugin(NetworkPlugin(networkMonitor))
            .registerPlugin(ConfigurationPlugin(this))
            .registerPlugin(ActionsPlugin(this))
            .registerPlugin(GooglePlayReferrerPlugin())
            .registerPlugin(FileChooserPlugin(this, externalCacheDir, "$packageName.provider"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1️⃣ Debug in emulator with a server running on the host machine on localhost:1234
        // Run `npm start` in `<racehorse>/web/example` then start the app in emulator.
        webView.loadApp("https://10.0.2.2:1234")
        setContentView(webView)

/*
        // 2️⃣ Load app bundle from src/main/assets folder
        // Run `num run build` in `<racehorse>/web/example`, copy files from `<racehorse>/web/example/dist` to
        // `<racehorse>/android/example/src/main/assets`, then start the app in emulator.
        webView.loadApp(
            "http://example.com",
            WebViewAssetLoader.Builder()
                .setDomain("example.com")
                .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
                .build()
        )
        setContentView(webView)
*/

/*
        // 3️⃣ Bootstrapper
        // Run `num run start:bootstrapper` in `<racehorse>/web/example` then start the app in emulator.
        //
        // If the bundle is downloaded via a non-secure request, then add `android:usesCleartextTraffic="true"`
        // attribute to `AndroidManifest.xml/manifest/application`. `BundleReadyEvent` is emitted after bundle is
        // successfully downloaded, see `onBundleReadyEvent` below.
        eventBus.register(this)
        Thread {
            RacehorseBootstrapper(File(filesDir, "app"), eventBus).start("0.0.0", true) {
                URL("http://10.0.2.2:1234/dist.zip").openConnection()
            }
        }.start()
*/

    }

    override fun onStart() {
        super.onStart()

        networkMonitor.start()
        webView.startPlugins()
    }

    override fun onPause() {
        super.onPause()

        networkMonitor.stop()
        webView.pausePlugins()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBundleReadyEvent(event: BundleReadyEvent) {
        webView.loadApp(
            "https://example.com",
            WebViewAssetLoader.Builder()
                .setDomain("example.com")
                .addPathHandler("/", StaticPathHandler(event.appDir))
                .build()
        )
        setContentView(webView)
    }
}
