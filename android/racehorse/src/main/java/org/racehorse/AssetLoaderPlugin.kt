package org.racehorse

import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import androidx.webkit.WebViewAssetLoader
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.ShouldInterceptRequestEvent
import org.racehorse.webview.ShouldOverrideUrlLoadingEvent
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

/**
 * Intercepts requests and serves the responses using an [assetLoader]. If a URL cannot be handled by the [assetLoader]
 * then it is opened in an external app by posting an [OpenInExternalApplicationEvent].
 *
 * @param assetLoader The asset loader that resolves the URL.
 * @param eventBus The event bus to which events are posted.
 */
open class AssetLoaderPlugin(
    private val assetLoader: WebViewAssetLoader,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    @Subscribe
    open fun onShouldInterceptRequest(event: ShouldInterceptRequestEvent) {
        if (event.response == null) {
            event.response = assetLoader.shouldInterceptRequest(event.request.url)
        }
    }

    @Subscribe
    open fun onShouldOverrideUrlLoading(event: ShouldOverrideUrlLoadingEvent) {
        if (assetLoader.shouldInterceptRequest(event.request.url) == null && event.shouldHandle()) {
            eventBus.post(OpenInExternalApplicationEvent(event.request.url.toString()))
        }
    }
}

/**
 * The path handler that loads static assets from a given directory.
 *
 * @param baseDir The directory from which files are served.
 * @param indexFileName The name of the index file to look for if handled path is a directory.
 */
open class StaticPathHandler(
    private val baseDir: File,
    private val indexFileName: String = "index.html"
) : WebViewAssetLoader.PathHandler {

    private val baseDirPath = baseDir.canonicalPath

    @WorkerThread
    override fun handle(path: String): WebResourceResponse {
        var file = File(baseDir, path)

        if (file.canonicalPath.startsWith(baseDirPath)) {
            if (file.isDirectory) {
                file = File(file, indexFileName)
            }
            if (file.isFile && file.canRead()) {
                return WebResourceResponse(URLConnection.guessContentTypeFromName(path), null, FileInputStream(file))
            }
        }

        return WebResourceResponse(null, null, null)
    }
}
