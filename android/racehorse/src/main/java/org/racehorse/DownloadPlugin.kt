package org.racehorse

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.requiresPermission
import org.racehorse.webview.DownloadStartEvent
import java.io.File
import java.io.Serializable
import java.util.Base64

class Download(
    /**
     * An identifier for a particular download, unique across the system. Clients use this ID to
     * make subsequent calls related to the download.
     */
    val id: Long,

    /**
     * The client-supplied title for this download. This will be displayed in system notifications.
     * Defaults to the empty string.
     */
    val title: String,

    /**
     * The client-supplied description of this download. This will be displayed in system
     * notifications. Defaults to the empty string.
     */
    val description: String,

    /**
     * The URI to be downloaded.
     */
    val uri: String,

    /**
     * Internet Media Type of the downloaded file. If no value is provided upon creation, this will
     * initially be null and will be filled in based on the server's response once the download has
     * started.
     */
    val mediaType: String,

    /**
     * Total size of the download in bytes. This will initially be -1 and will be filled in once
     * the download starts.
     */
    val totalSizeBytes: Long,

    /**
     * The URI where downloaded file will be stored. If a destination is supplied by client, that URI
     * will be used here. Otherwise, the value will initially be null and will be filled in with a
     * generated URI once the download has started.
     */
    val localUri: String,

    /**
     * Current status of the download.
     */
    val status: Int,

    /**
     * Provides more detail on the status of the download. Its meaning depends on the value of [status].
     *
     * When [status] is [DownloadManager.STATUS_FAILED], this indicates the type of error that
     * occurred. If an HTTP error occurred, this will hold the HTTP status code as defined in RFC
     * 2616. Otherwise, it will hold one of the ERROR_* constants.
     *
     * When [status] is [DownloadManager.STATUS_PAUSED], this indicates why the download is
     * paused. It will hold one of the PAUSED_* constants.
     *
     * If [status] is neither [DownloadManager.STATUS_FAILED] nor [DownloadManager.STATUS_PAUSED],
     * this column's value is undefined.
     */
    val reason: Int,

    /**
     * Number of bytes download so far.
     */
    val bytesDownloadedSoFar: Long,

    /**
     * Timestamp when the download was last modified (wall clock time in UTC).
     */
    val lastModifiedTimestamp: Int,
) : Serializable {
    @SuppressLint("Range")
    constructor(cursor: Cursor) : this(
        id = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID)),
        title = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)),
        description = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)),
        uri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_URI)),
        mediaType = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE)),
        totalSizeBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
        localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)),
        status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)),
        reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON)),
        bytesDownloadedSoFar = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
        lastModifiedTimestamp = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)),
    )
}

/**
 * Starts a file download.
 */
class DownloadEvent(
    /**
     * The full URI of the content that should be downloaded. Supports HTTP, HTTPS, and data URI.
     */
    val uri: String,

    /**
     * The file name added to the downloads.
     */
    val fileName: String? = null,

    /**
     * The mimetype of the content reported by the server.
     */
    val mimeType: String? = null,

    /**
     * Set the title of this download, to be displayed in notifications. If no title is given, a default one will be
     * assigned based on the download filename, once the download starts.
     */
    val title: String? = null,

    /**
     * Set a description of this download, to be displayed in notifications.
     */
    val description: String? = null,

    /**
     * HTTP headers to be included with the download request.
     */
    val headers: Map<String, Array<String>>? = null,
) : RequestEvent() {
    class ResultEvent(
        /**
         * The ID of the scheduled download or -1 if data URI was instantly written to file.
         */
        val id: Long
    ) : ResponseEvent()
}

/**
 * Returns a previously started file download by its ID.
 */
class GetDownloadByIdEvent(var id: Long) : RequestEvent() {
    class ResultEvent(val download: Download?) : ResponseEvent()
}

/**
 * Returns all file downloads.
 */
class GetAllDownloadsEvent : RequestEvent() {
    class ResultEvent(val downloads: Array<Download>) : ResponseEvent()
}

open class DownloadPlugin(private val activity: ComponentActivity) {

    private val downloadManager by lazy { activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    @Subscribe
    open fun onDownloadStart(event: DownloadStartEvent) {
        val uri = Uri.parse(event.url)
        val fileName = URLUtil.guessFileName(event.url, event.contentDisposition, event.mimeType)

        if (uri.scheme == "data") {
            writeDataUri(uri, fileName)
            return
        }

        downloadManager.enqueue(DownloadManager.Request(uri).apply {
            allowScanningByMediaScanner()
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType(event.mimeType)
        })
    }

    @Subscribe
    open fun onDownload(event: DownloadEvent) {
        val uri = Uri.parse(event.uri)
        val fileName = event.fileName ?: URLUtil.guessFileName(event.uri, null, event.mimeType)

        if (uri.scheme == "data") {
            writeDataUri(uri, fileName)
            event.respond(DownloadEvent.ResultEvent(-1))
            return
        }

        val id = downloadManager.enqueue(DownloadManager.Request(uri).apply {
            allowScanningByMediaScanner()
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType(event.mimeType)
            setTitle(event.title)
            setDescription(event.description)

            event.headers?.forEach { (key, values) ->
                values.forEach { addRequestHeader(key, it) }
            }
        })

        event.respond(DownloadEvent.ResultEvent(id))
    }

    @Subscribe
    fun onGetDownloadById(event: GetDownloadByIdEvent) {
        event.respond(GetDownloadByIdEvent.ResultEvent(
            downloadManager.query(DownloadManager.Query().setFilterById(event.id)).use { cursor ->
                if (cursor.moveToFirst()) Download(cursor) else null
            }
        ))
    }

    @Subscribe
    fun onGetAllDownloads(event: GetAllDownloadsEvent) {
        event.respond(GetAllDownloadsEvent.ResultEvent(buildList {
            downloadManager.query(DownloadManager.Query()).use { cursor ->
                while (cursor.moveToNext()) {
                    add(Download(cursor))
                }
            }
        }.toTypedArray()))
    }

    private fun writeDataUri(dataUri: Uri, fileName: String) {
        val parts = dataUri.schemeSpecificPart.split(';', ',')
        val mimeType = parts.first()
        val data = Base64.getDecoder().decode(parts.last())

        // MediaStore
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.DownloadColumns.DISPLAY_NAME, fileName)
                put(MediaStore.DownloadColumns.MIME_TYPE, mimeType)
                put(MediaStore.DownloadColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = activity.contentResolver
            val fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return

            resolver.openOutputStream(fileUri).use { outputStream ->
                outputStream?.write(data)
            }
            return
        }

        // ExternalStorage
        activity.requiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val file = File(downloadsDir, fileName).let { file ->
                if (file.exists()) {
                    File.createTempFile(file.nameWithoutExtension, file.extension, downloadsDir)
                } else {
                    file
                }
            }

            file.writeBytes(data)

            downloadManager.addCompletedDownload(
                file.name,
                "",
                true,
                mimeType,
                file.absolutePath,
                file.length(),
                true
            )
        }
    }
}
