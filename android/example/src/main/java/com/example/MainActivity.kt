package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.webkit.WebViewAssetLoader
import com.facebook.FacebookSdk
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.*
import org.racehorse.evergreen.BundleReadyEvent
import org.racehorse.evergreen.EvergreenPlugin
import org.racehorse.evergreen.UpdateMode
import org.racehorse.webview.RacehorseWebChromeClient
import org.racehorse.webview.RacehorseWebViewClient
import java.io.File
import java.net.URL

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
        eventBus.register(DeepLinkPlugin())
        eventBus.register(PermissionsPlugin(this))
        eventBus.register(NotificationsPlugin(this))
        eventBus.register(GoogleSignInPlugin(this))
        eventBus.register(FacebookLoginPlugin(this))
        eventBus.register(FacebookSharePlugin(this))
        eventBus.register(ToastPlugin(this))

        FacebookSdk.sdkInitialize(this)

        // Run `npm run watch` in `<racehorse>/web/example` to build the web app and start the server.

        if (BuildConfig.DEBUG) {
            // 1️⃣ Live reload
            //
            // Debug in emulator with a server running on the host machine on localhost:10001

            webView.loadUrl("http://10.0.2.2:10001")

            setContentView(webView)
        } else {
            // 2️⃣ Evergreen
            //
            // An update bundle `web/example/dist/bundle.zip` is downloaded using the `EvergreenPlugin` and served from
            // the internal app cache.
            //
            // If the bundle is downloaded via a non-secure request, then add `android:usesCleartextTraffic="true"`
            // attribute to `AndroidManifest.xml/manifest/application`. `BundleReadyEvent` is emitted after bundle is
            // successfully downloaded, see `onBundleReady` below.

            val evergreenPlugin = EvergreenPlugin(File(filesDir, "app"))

            eventBus.register(this)
            eventBus.register(evergreenPlugin)

            Thread {
                // The update bundle is downloaded if there's no bundle available, or if provided version differs from
                // the version of previously downloaded bundle.
                evergreenPlugin.start("0.0.0", UpdateMode.MANDATORY) {
                    URL("http://10.0.2.2:10001/bundle.zip").openConnection()
                }
            }.start()
        }
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

    override fun onNewIntent(intent: Intent?) {
        if (intent != null) {
            EventBus.getDefault().post(OpenDeepLinkEvent(intent))
        }
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
