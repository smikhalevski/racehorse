package com.example.myapplication

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
import org.racehorse.StaticPathHandler
import org.racehorse.webview.RacehorseWebChromeClient
import org.racehorse.webview.RacehorseWebViewClient
import java.io.File

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

    private val configurationController: ConfigurationController by lazy {
        ConfigurationController(this)
    }

    private val connectionController: ConnectionController by lazy {
        ConnectionController(webView).apply {
            enable()
        }
    }

    private val networkController: NetworkController by lazy {
        NetworkController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EventBus.getDefault().let {
            it.register(ActionsController(this))
            it.register(configurationController)
            it.register(connectionController)
            it.register(EncryptedKeyValueStorageController(this, File(filesDir, "storage")))
            it.register(FileChooserController(this, externalCacheDir, "$packageName.provider"))
            it.register(FirebaseController())
            it.register(GooglePlayReferrerController(this))
            it.register(HttpsController())
            it.register(networkController)
            it.register(PermissionsController(this))

            it.register(ToastController(this))
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
            StaticAssetsController(
                "http://example.com",
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

        val evergreenController = EvergreenController(File(filesDir, "app"))

        EventBus.getDefault().let {
            it.register(this)
            it.register(evergreenController)
        }

        Thread {
            evergreenController.start("0.0.0", true) { URL("http://10.0.2.2:1234/dist.zip").openConnection() }
        }.start()
*/
    }

    override fun onStart() {
        super.onStart()
        configurationController.enable()
        networkController.enable()
    }

    override fun onPause() {
        super.onPause()
        cookieManager.flush()
        configurationController.disable()
        networkController.disable()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBundleReadyEvent(event: BundleReadyEvent) {
        EventBus.getDefault().register(
            StaticAssetsController(
                "http://example.com",
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
