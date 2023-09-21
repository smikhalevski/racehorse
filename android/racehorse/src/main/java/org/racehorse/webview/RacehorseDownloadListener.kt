package org.racehorse.webview

import android.webkit.DownloadListener
import android.webkit.WebView
import org.greenrobot.eventbus.EventBus

/**
 * Notifies the host application that a file should be downloaded.
 */
class DownloadStartEvent(
    /**
     * The full URL to the content that should be downloaded.
     */
    val url: String,

    /**
     * The user agent to be used for the download.
     */
    val userAgent: String,

    /**
     * Content-disposition http header, if present.
     */
    val contentDisposition: String,

    /**
     * The mimetype of the content reported by the server.
     */
    val mimeType: String,

    /**
     * The file size reported by the server.
     */
    val contentLength: Long
)

/**
 * Posts [DownloadStartEvent] when the [WebView] requests to start a file download.
 */
open class RacehorseDownloadListener(private val eventBus: EventBus = EventBus.getDefault()) : DownloadListener {

    override fun onDownloadStart(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        eventBus.post(DownloadStartEvent(url, userAgent, contentDisposition, mimeType, contentLength))
    }
}
