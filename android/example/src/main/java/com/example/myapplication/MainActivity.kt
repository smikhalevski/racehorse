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

    private val webView: WebView by lazy {
        WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.setGeolocationEnabled(true)

            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            webChromeClient = RacehorseWebChromeClient()
            webViewClient = RacehorseWebViewClient()
        }
    }

    private val cookieManager: CookieManager by lazy {
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(webView, true)
        }
    }

    private val networkPlugin: NetworkPlugin by lazy {
        NetworkPlugin(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().let {
            it.register(EventBridge(webView))
            it.register(DevicePlugin(this))
            it.register(EncryptedStoragePlugin(File(filesDir, "storage"), BuildConfig.APPLICATION_ID.toByteArray()))
            it.register(FileChooserPlugin(this, externalCacheDir, "${BuildConfig.APPLICATION_ID}.provider"))
            it.register(FirebasePlugin())
            it.register(GooglePlayReferrerPlugin(this))
            it.register(HttpsPlugin())
            it.register(networkPlugin)
            it.register(KeyboardPlugin(this))
            it.register(IntentsPlugin(this))
            it.register(PermissionsPlugin(this))
            it.register(NotificationsPlugin(this))

            it.register(ToastPlugin(this))
        }

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

        EventBus.getDefault().let {
            it.register(this)
            it.register(evergreenPlugin)
        }

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
