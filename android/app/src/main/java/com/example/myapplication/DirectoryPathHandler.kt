package com.example.myapplication

import android.webkit.WebResourceResponse
import androidx.annotation.WorkerThread
import androidx.webkit.WebViewAssetLoader
import java.io.File
import java.io.FileInputStream
import java.net.URLConnection

/**
 * The path handler that loads static assets from a given directory.
 */
class DirectoryPathHandler(private val baseDirectory: File) : WebViewAssetLoader.PathHandler {

    private val basePath = baseDirectory.absolutePath

    init {
        require(baseDirectory.isDirectory && baseDirectory.canRead()) {
            "Cannot serve contents of a directory ${baseDirectory.absolutePath}"
        }
    }

    @WorkerThread
    override fun handle(path: String): WebResourceResponse {
        var file = File(baseDirectory, path)

        if (file.absolutePath.startsWith(basePath)) {
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
