package org.racehorse.webview

import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

/**
 * The path handler that loads static assets from a given directory.
 *
 * @param baseDir The directory from which files are served.
 * @param indexFileName The name of the index file to look for if handled path is a directory.
 */
class StaticPathHandler(val baseDir: File, val indexFileName: String = "index.html") : WebViewAssetLoader.PathHandler {

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
