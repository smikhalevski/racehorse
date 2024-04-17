package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.facebook.FacebookSdk
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.ActivityPlugin
import org.racehorse.AssetLoaderPlugin
import org.racehorse.BiometricEncryptedStoragePlugin
import org.racehorse.BiometricPlugin
import org.racehorse.ContactsPlugin
import org.racehorse.DeepLinkPlugin
import org.racehorse.DevicePlugin
import org.racehorse.DownloadPlugin
import org.racehorse.EncryptedStoragePlugin
import org.racehorse.EventBridge
import org.racehorse.FacebookLoginPlugin
import org.racehorse.FacebookSharePlugin
import org.racehorse.FileChooserPlugin
import org.racehorse.FirebasePlugin
import org.racehorse.FsPlugin
import org.racehorse.GalleryCameraFileFactory
import org.racehorse.GooglePlayReferrerPlugin
import org.racehorse.GoogleSignInPlugin
import org.racehorse.HttpsPlugin
import org.racehorse.KeyboardPlugin
import org.racehorse.NetworkPlugin
import org.racehorse.NotificationsPlugin
import org.racehorse.OpenDeepLinkEvent
import org.racehorse.PermissionsPlugin
import org.racehorse.ProxyPathHandler
import org.racehorse.StaticPathHandler
import org.racehorse.evergreen.BundleReadyEvent
import org.racehorse.evergreen.EvergreenPlugin
import org.racehorse.evergreen.UpdateMode
import org.racehorse.webview.RacehorseDownloadListener
import org.racehorse.webview.RacehorseWebChromeClient
import org.racehorse.webview.RacehorseWebViewClient
import java.io.File
import java.net.URL

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : AppCompatActivity() {

    private val webView by lazy { WebView(this) }
    private val eventBus = EventBus.getDefault()
    private val networkPlugin = NetworkPlugin(this)
    private val assetLoaderPlugin = AssetLoaderPlugin(this)
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
        webView.setDownloadListener(RacehorseDownloadListener())

        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Renders "Hello" in the iframe
        assetLoaderPlugin.registerAssetLoader("https://iframe.example.com") {
            WebResourceResponse("text/html", null, "Hello".byteInputStream())
        }

        eventBus.register(EventBridge(webView).apply { enable() })
        eventBus.register(assetLoaderPlugin)
        eventBus.register(DevicePlugin(this))
        eventBus.register(EncryptedStoragePlugin(File(filesDir, "storage"), BuildConfig.APPLICATION_ID.toByteArray()))
        eventBus.register(
            FileChooserPlugin(
                this,
                GalleryCameraFileFactory(this, cacheDir, "${BuildConfig.APPLICATION_ID}.provider")
            )
        )
        eventBus.register(DownloadPlugin(this))
        eventBus.register(FirebasePlugin())
        eventBus.register(GooglePlayReferrerPlugin(this))
        eventBus.register(HttpsPlugin())
        eventBus.register(networkPlugin)
        eventBus.register(KeyboardPlugin(this))
        eventBus.register(ActivityPlugin(this).apply { enable() })
        eventBus.register(DeepLinkPlugin())
        eventBus.register(PermissionsPlugin(this))
        eventBus.register(NotificationsPlugin(this))
        eventBus.register(GoogleSignInPlugin(this))
        eventBus.register(FacebookLoginPlugin(this))
        eventBus.register(FacebookSharePlugin(this))
        eventBus.register(BiometricPlugin(this))
        eventBus.register(BiometricEncryptedStoragePlugin(this, File(filesDir, "biometric_storage")))
        eventBus.register(ContactsPlugin(this))
        eventBus.register(FsPlugin(this, providerAuthority = "${BuildConfig.APPLICATION_ID}.provider"))

        // From the example app
        eventBus.register(ToastPlugin(this))

        @Suppress("DEPRECATION")
        FacebookSdk.sdkInitialize(this)

        // 🟡 Run `npm run watch` in `<racehorse>/web/example` to build the web app and start the server.

        if (BuildConfig.DEBUG) {
            // 1️⃣ Live reload

            // Rollup starts server on localhost:10001
            assetLoaderPlugin.registerAssetLoader("https://example.com", ProxyPathHandler("http://10.0.2.2:10001"))

            // Example app uses livereload that is loaded from http://10.0.2.2:35729, since the app is rendered using
            // https://example.com which uses HTTPS, live reload is rejected because of the mixed content policy.
            // Don't use this setting in production!
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            webView.loadUrl("https://example.com")

            setContentView(webView)
        } else {
            // 2️⃣ Evergreen

            // An update bundle `<racehorse>/web/example/dist/bundle.zip` is downloaded using the `EvergreenPlugin` and
            // served from the internal app cache.
            //
            // If the bundle is downloaded via a non-secure request, then add `android:usesCleartextTraffic="true"`
            // attribute to `AndroidManifest.xml/manifest/application`. `BundleReadyEvent` is emitted after bundle is
            // successfully downloaded, see `onBundleReady` below.

            val evergreenPlugin = EvergreenPlugin(File(filesDir, "app"))

            eventBus.register(this)
            eventBus.register(evergreenPlugin)

            // Download the bundle in the background thread.
            Thread {
                // The update bundle is downloaded if there's no bundle available, or if the provided version differs
                // from the version of previously downloaded bundle.
                evergreenPlugin.start("0.0.0", UpdateMode.MANDATORY) {
                    URL("http://10.0.2.2:10001/bundle.zip").openConnection()
                }
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()

        networkPlugin.enable()
    }

    override fun onPause() {
        super.onPause()

        cookieManager.flush()
        networkPlugin.disable()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent != null) {
            EventBus.getDefault().post(OpenDeepLinkEvent(intent))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBundleReady(event: BundleReadyEvent) {
        assetLoaderPlugin.registerAssetLoader("https://example.com", StaticPathHandler(event.appDir))

        webView.loadUrl("https://example.com")

        setContentView(webView)
    }
}
