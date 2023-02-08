package org.racehorse.webview

import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

/**
 * The path handler that loads static assets from a given directory.
 */
class StaticPathHandler(private val baseDir: File) : WebViewAssetLoader.PathHandler {

    private val baseDirPath = baseDir.canonicalPath

    @WorkerThread
    override fun handle(path: String): WebResourceResponse {
        var file = File(baseDir, path)

        if (file.canonicalPath.startsWith(baseDirPath)) {
            if (file.isDirectory) {
                file = File(file, "index.html")
            }
            if (file.isFile && file.canRead()) {
                return WebResourceResponse(URLConnection.guessContentTypeFromName(path), null, FileInputStream(file))
            }
        }

        return WebResourceResponse(null, null, null)
    }
}
