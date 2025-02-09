package com.example

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
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
import org.racehorse.evergreen.StartEvent
import org.racehorse.evergreen.UpdateMode
import org.racehorse.webview.RacehorseDownloadListener
import org.racehorse.webview.RacehorseWebChromeClient
import org.racehorse.webview.RacehorseWebViewClient
import java.io.File
import java.net.URL
import java.util.Date

const val APP_URL = "https://example.local"

const val DEV_SERVER_URL = "http://10.0.2.2:10001"

@SuppressLint("SetJavaScriptEnabled")
class MainActivity : AppCompatActivity() {

    private val webView by lazy { WebView(this) }
    private val eventBus = EventBus.getDefault()
    private val networkPlugin = NetworkPlugin(this)
    private val assetLoaderPlugin = AssetLoaderPlugin(this)
    private val cookieManager = CookieManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        // https://developer.android.com/develop/ui/views/layout/edge-to-edge
        enableEdgeToEdge()

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
        assetLoaderPlugin.registerAssetLoader("https://guestcontent.local") {
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
        eventBus.register(KeyboardPlugin(this).apply { enable() })
        eventBus.register(ActivityPlugin(this).apply { enable() })
        eventBus.register(DeepLinkPlugin())
        eventBus.register(PermissionsPlugin(this))
        eventBus.register(NotificationsPlugin(this))
        eventBus.register(GoogleSignInPlugin(this))
        eventBus.register(FacebookLoginPlugin(this).also {
            @Suppress("DEPRECATION")
            FacebookSdk.sdkInitialize(this)
        })
        eventBus.register(FacebookSharePlugin(this))
        eventBus.register(BiometricPlugin(this))
        eventBus.register(BiometricEncryptedStoragePlugin(this, File(filesDir, "biometric_storage")))
        eventBus.register(ContactsPlugin(this))
        eventBus.register(
            FsPlugin(
                activity = this,
                providerAuthority = "${BuildConfig.APPLICATION_ID}.provider",
                baseLocalUrl = "$APP_URL/fs"
            )
        )
        eventBus.register(ToastPlugin(this))

        // 🟡 Run `npm start` in `<racehorse>/web/example` to build the web app and start the server.

        if (BuildConfig.DEBUG) {
            // 1️⃣ Live reload

            // Example app uses livereload that connects to ws://10.0.2.2:10001, since the app is rendered using
            // https://example.local which uses HTTPS, this connection is rejected because of the mixed content policy.
            // Don't use this setting in production!
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            assetLoaderPlugin.registerAssetLoader(APP_URL, ProxyPathHandler(DEV_SERVER_URL))

            webView.loadUrl(APP_URL)

            setContentView(webView)
        } else {
            // 2️⃣ Evergreen

            // An update bundle `<racehorse>/web/example/dist/bundle.zip` is downloaded using the `EvergreenPlugin` and
            // served from the internal app cache.
            //
            // If the bundle is downloaded via a non-secure request, then add `android:usesCleartextTraffic="true"`
            // attribute to `AndroidManifest.xml/manifest/application`. `BundleReadyEvent` is emitted after bundle is
            // successfully downloaded, see `onBundleReady` below.

            eventBus.register(EvergreenPlugin(File(filesDir, "app")))
            eventBus.register(this)

            // The update bundle is downloaded if there's no bundle available, or if the available version differs
            // from the version of previously downloaded bundle.
            eventBus.post(StartEvent(version = "0.0.0+" + Date().time.toString(), UpdateMode.MANDATORY) {
                URL("$DEV_SERVER_URL/bundle.zip").openConnection()
            })
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        EventBus.getDefault().post(OpenDeepLinkEvent(intent))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBundleReady(event: BundleReadyEvent) {
        assetLoaderPlugin.registerAssetLoader(APP_URL, StaticPathHandler(event.appDir))

        webView.loadUrl(APP_URL)

        setContentView(webView)
    }
}
