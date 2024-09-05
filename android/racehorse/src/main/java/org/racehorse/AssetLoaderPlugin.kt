package org.racehorse

import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import androidx.annotation.WorkerThread
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.PathHandler
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.guessIntentAction
import org.racehorse.utils.ifNullOrBlank
import org.racehorse.utils.launchActivity
import org.racehorse.webview.ShouldInterceptRequestEvent
import org.racehorse.webview.ShouldOverrideUrlLoadingEvent
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.TreeMap

/**
 * Intercepts requests and serves the responses using an registered asset loaders.
 *
 * @param activity The activity that starts the external app if no asset loaders can handle the intercepted URL.
 */
open class AssetLoaderPlugin(private val activity: ComponentActivity) {

    /**
     * If `true` then URLs that cannot be handled by registered asset loaders are opened in an external browser app.
     */
    var isUnhandledRequestOpenedInExternalBrowser = true

    private val assetLoaders = LinkedHashMap<WebViewAssetLoader, Uri?>()

    /**
     * Registers an asset loader.
     */
    fun registerAssetLoader(assetLoader: WebViewAssetLoader) = assetLoaders.set(assetLoader, null)

    /**
     * Registers a new asset loader that uses a handler for a given URL.
     */
    fun registerAssetLoader(uri: String, handler: PathHandler) = registerAssetLoader(Uri.parse(uri), handler)

    /**
     * Registers a new asset loader that uses a handler for a given URL.
     */
    fun registerAssetLoader(uri: Uri, handler: PathHandler) = assetLoaders.set(
        WebViewAssetLoader.Builder()
            .setHttpAllowed(uri.scheme == "http")
            .setDomain(uri.authority ?: WebViewAssetLoader.DEFAULT_DOMAIN)
            .addPathHandler(uri.path.ifNullOrBlank { "/" }, handler)
            .build(),
        uri
    )

    /**
     * Unregisters an asset loader.
     */
    fun unregisterAssetLoader(assetLoader: WebViewAssetLoader) = assetLoaders.remove(assetLoader)

    /**
     * Unregisters all assets loaders that were registered for a given URI.
     */
    fun unregisterAssetLoaders(uri: String) = unregisterAssetLoaders(Uri.parse(uri))

    /**
     * Unregisters all assets loaders that were registered for a given URI.
     */
    fun unregisterAssetLoaders(uri: Uri) = assetLoaders.values.removeAll { it == uri }

    @Subscribe
    open fun onShouldInterceptRequest(event: ShouldInterceptRequestEvent) {
        if (event.response == null) {
            event.response = assetLoaders.firstNotNullOfOrNull { it.key.shouldInterceptRequest(event.request.url) }
        }
    }

    @Subscribe
    open fun onShouldOverrideUrlLoading(event: ShouldOverrideUrlLoadingEvent) {
        val url = event.request.url

        if (
            isUnhandledRequestOpenedInExternalBrowser &&
            assetLoaders.all { it.key.shouldInterceptRequest(url) == null } &&
            event.shouldHandle()
        ) {
            activity.launchActivity(Intent(url.guessIntentAction(), url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/**
 * The path handler that loads static assets from a given directory.
 *
 * @param baseDir The directory on the device from which files are served.
 * @param indexFileName The name of the index file to look for if handled path is a directory.
 */
open class StaticPathHandler(
    private val baseDir: File,
    private val indexFileName: String = "index.html"
) : PathHandler {

    private val baseDirPath = baseDir.canonicalPath

    @WorkerThread
    override fun handle(path: String): WebResourceResponse {
        var file = File(baseDir, path)

        if (file.canonicalPath.startsWith(baseDirPath)) {
            if (file.isDirectory) {
                file = File(file, indexFileName)
            }
            if (file.isFile && file.canRead()) {
                val mimeType = URLConnection.guessContentTypeFromName(path)
                return WebResourceResponse(mimeType, null, FileInputStream(file))
            }
        }

        return WebResourceResponse(null, null, null)
    }
}

/**
 * Redirects content from the given URL.
 */
open class ProxyPathHandler(private val baseUrl: URL) : PathHandler {

    constructor(baseUrl: String) : this(URL(baseUrl))

    override fun handle(path: String): WebResourceResponse {
        val connection = openConnection(path)

        val headers = connection.headerFields
            .toMutableMap()
            .apply { remove(null) }
            .mapValuesTo(TreeMap(String.CASE_INSENSITIVE_ORDER)) { it.value.joinToString("; ") }

        headers.remove("Content-Type")
        headers.remove("Content-Length")

        val mimeType = connection.contentType?.substringBefore(';')

        val inputStream = try {
            connection.inputStream
        } catch (_: IOException) {
            null
        }

        return WebResourceResponse(
            mimeType,
            connection.contentEncoding,
            connection.responseCode,
            connection.responseMessage,
            headers,
            inputStream
        )
    }

    open fun openConnection(path: String): HttpURLConnection {
        val connection = URL(baseUrl, path).openConnection() as HttpURLConnection

        connection.instanceFollowRedirects = true
        connection.connectTimeout = 10_000

        return connection
    }
}
