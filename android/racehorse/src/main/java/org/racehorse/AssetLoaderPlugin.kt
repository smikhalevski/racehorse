package org.racehorse

import android.content.Intent
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import androidx.annotation.WorkerThread
import androidx.webkit.WebViewAssetLoader
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.guessIntentAction
import org.racehorse.utils.launchActivity
import org.racehorse.webview.ShouldInterceptRequestEvent
import org.racehorse.webview.ShouldOverrideUrlLoadingEvent
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

/**
 * Intercepts requests and serves the responses using an [assetLoader], or starts a new activity.
 *
 * @param activity The activity that starts the external app if [assetLoader] cannot handle the intercepted URL.
 * @param assetLoader The asset loader that resolves the URL.
 */
open class AssetLoaderPlugin(private val activity: ComponentActivity, private val assetLoader: WebViewAssetLoader) {

    @Subscribe
    open fun onShouldInterceptRequest(event: ShouldInterceptRequestEvent) {
        if (event.response == null) {
            event.response = assetLoader.shouldInterceptRequest(event.request.url)
        }
    }

    @Subscribe
    open fun onShouldOverrideUrlLoading(event: ShouldOverrideUrlLoadingEvent) {
        val url = event.request.url

        if (assetLoader.shouldInterceptRequest(url) == null && event.shouldHandle()) {
            activity.launchActivity(Intent(url.guessIntentAction(), url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
