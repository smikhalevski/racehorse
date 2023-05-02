package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.*
import org.racehorse.evergreen.BundleReadyEvent
import org.racehorse.webview.RacehorseWebChromeClient
import org.racehorse.webview.RacehorseWebViewClient
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : AppCompatActivity() {

    private val webView by lazy { WebView(this) }
    private val eventBus = EventBus.getDefault()
    private val networkPlugin = NetworkPlugin(this)
    private val cookieManager = CookieManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false
        webView.settings.setGeolocationEnabled(true)

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webChromeClient = RacehorseWebChromeClient()
        webView.webViewClient = RacehorseWebViewClient()

        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        eventBus.register(EventBridge(webView))
        eventBus.register(DevicePlugin(this))
        eventBus.register(EncryptedStoragePlugin(File(filesDir, "storage"), BuildConfig.APPLICATION_ID.toByteArray()))
        eventBus.register(FileChooserPlugin(this, externalCacheDir, "${BuildConfig.APPLICATION_ID}.provider"))
        eventBus.register(FirebasePlugin())
        eventBus.register(GooglePlayReferrerPlugin(this))
        eventBus.register(HttpsPlugin())
        eventBus.register(networkPlugin)
        eventBus.register(KeyboardPlugin(this))
        eventBus.register(ActivityPlugin(this))
        eventBus.register(PermissionsPlugin(this))
        eventBus.register(NotificationsPlugin(this))
        eventBus.register(ToastPlugin(this))

        // 1️⃣ Debug in emulator with a server running on the host machine on localhost:1234
        // Run `npm start` in `<racehorse>/web/example` then start the app in emulator.
        webView.loadUrl("https://10.0.2.2:1234")

        setContentView(webView)

        /*
        // 2️⃣ Load app bundle from src/main/assets folder
        // Run `num run build` in `<racehorse>/web/example`, copy files from `<racehorse>/web/example/dist` to
        // `<racehorse>/android/example/src/main/assets`, then start the app in emulator.

        EventBus.getDefault().register(
            AssetLoaderPlugin(
                this,
                WebViewAssetLoader.Builder()
                    .setDomain("example.com")
                    .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
                    .build()
            )
        )

        webView.loadUrl("http://example.com")

        setContentView(webView)
        */

        /*
        // 3️⃣ Evergreen
        // Run `num run start:evergreen` in `<racehorse>/web/example` then start the app in emulator.
        //
        // If the bundle is downloaded via a non-secure request, then add `android:usesCleartextTraffic="true"`
        // attribute to `AndroidManifest.xml/manifest/application`. `BundleReadyEvent` is emitted after bundle is
        // successfully downloaded, see `onBundleReadyEvent` below.

        val evergreenPlugin = EvergreenPlugin(File(filesDir, "app"))

        eventBus.register(this)
        eventBus.register(evergreenPlugin)

        Thread {
            evergreenPlugin.start("0.0.0", true) { URL("http://10.0.2.2:1234/dist.zip").openConnection() }
        }.start()
        */
    }

    override fun onStart() {
        super.onStart()

        networkPlugin.enable()
    }

    override fun onPause() {
        super.onPause()

        cookieManager.flush()
        networkPlugin.disable()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBundleReady(event: BundleReadyEvent) {
        EventBus.getDefault().register(
            AssetLoaderPlugin(
                this,
                WebViewAssetLoader.Builder()
                    .setDomain("example.com")
                    .addPathHandler("/", StaticPathHandler(event.appDir))
                    .build()
            )
        )

        webView.loadUrl("https://example.com")

        setContentView(webView)
    }
}
