package org.racehorse

import android.Manifest
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.core.database.getStringOrNull
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.askForPermission
import org.racehorse.webview.DownloadStartEvent
import java.io.File
import java.io.Serializable
import java.util.Base64

class Download(
    /**
     * The download manager that handles the download.
     */
    downloadManager: DownloadManager,

    /**
     * An identifier for a particular download, unique across the system. Clients use this ID to make subsequent calls
     * related to the download.
     */
    val id: Long,

    /**
     * Current status of the download.
     */
    val status: Int,

    /**
     * Provides more detail on the status of the download. Its meaning depends on the value of [status].
     *
     * When [status] is [DownloadManager.STATUS_FAILED], this indicates the type of error that occurred. If an HTTP
     * error occurred, this will hold the HTTP status code as defined in RFC 2616. Otherwise, it will hold one of the
     * ERROR_* constants.
     *
     * When [status] is [DownloadManager.STATUS_PAUSED], this indicates why the download is paused. It will hold one of
     * the PAUSED_* constants.
     *
     * If [status] is neither [DownloadManager.STATUS_FAILED] nor [DownloadManager.STATUS_PAUSED], this column's value
     * is undefined.
     */
    val reason: Int,

    /**
     * The URI to be downloaded.
     */
    val uri: String,

    /**
     * The URI where downloaded file will be stored. If a destination is supplied by client, that URI will be used here.
     * Otherwise, the value will initially be null and will be filled in with a generated URI once the download has
     * started.
     */
    val localUri: String?,

    /**
     * The MIME type of the downloaded file.
     */
    val mimeType: String?,

    /**
     * Total size of the download in bytes. This will initially be -1 and will be filled in once the download starts.
     */
    val totalSize: Long,

    /**
     * The number of bytes download so far.
     */
    val downloadedSize: Long,

    /**
     * Timestamp when the download was last modified (wall clock time in UTC).
     */
    val lastModifiedTimestamp: Int,
) : Serializable {

    /**
     * The `content:` URI of the downloaded file.
     */
    val contentUri = downloadManager.getUriForDownloadedFile(id)?.toString()

    constructor(downloadManager: DownloadManager, cursor: Cursor) : this(
        downloadManager,

        id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)),
        status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
        reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
        uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)),
        localUri = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)),
        mimeType = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)),
        totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
        downloadedSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
        lastModifiedTimestamp = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)),
    )
}

/**
 * Adds a new download.
 */
class AddDownloadEvent(
    /**
     * The full URI of the content that should be downloaded. Supports HTTP, HTTPS, and data URI.
     */
    val uri: String,

    /**
     * The file name added to the downloads.
     */
    val fileName: String? = null,

    /**
     * The MIME type of the downloaded file.
     */
    val mimeType: String? = null,

    /**
     * HTTP headers to be included with the download request.
     */
    val headers: List<Pair<String, String>>? = null,
) : RequestEvent() {
    class ResultEvent(val id: Long) : ResponseEvent()
}

/**
 * Returns a previously started file download by its ID.
 */
class GetDownloadEvent(var id: Long) : RequestEvent() {
    class ResultEvent(val download: Download?) : ResponseEvent()
}

/**
 * Returns all file downloads.
 */
class GetAllDownloadsEvent : RequestEvent() {
    class ResultEvent(val downloads: List<Download>) : ResponseEvent()
}

/**
 * Cancel a download and remove it from the download manager. Download will be stopped if it was running, and it will no
 * longer be accessible through the download manager. If there is a downloaded file, partial or complete, it is deleted.
 */
class RemoveDownloadEvent(var id: Long) : RequestEvent() {
    class ResultEvent(val removed: Boolean) : ResponseEvent()
}

open class DownloadPlugin(private val activity: ComponentActivity) {

    companion object {
        private val SAVE_TO_DIR = Environment.DIRECTORY_DOWNLOADS

        private const val FALLBACK_MIME_TYPE = "application/octet-stream"
        private const val FALLBACK_FILE_NAME = "data."
        private const val FALLBACK_EXTENSION = "bin"
    }

    private val downloadManager by lazy { activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onDownloadStart(event: DownloadStartEvent) {
        onAddDownload(
            AddDownloadEvent(
                uri = event.url,
                fileName = URLUtil.guessFileName(event.url, event.contentDisposition, event.mimeType),
                mimeType = event.mimeType
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onAddDownload(event: AddDownloadEvent) {
        val uri = Uri.parse(event.uri)

        when (uri.scheme?.lowercase()) {
            "data" -> addDownload(event, DataUri(uri))

            "http", "https" -> enqueueDownload(event, uri)

            else -> throw IllegalArgumentException("Unsupported URI")
        }
    }

    @Subscribe
    fun onGetDownload(event: GetDownloadEvent) {
        event.respond(GetDownloadEvent.ResultEvent(
            downloadManager.query(DownloadManager.Query().setFilterById(event.id)).use { cursor ->
                if (cursor.moveToFirst()) Download(downloadManager, cursor) else null
            }
        ))
    }

    @Subscribe
    fun onGetAllDownloads(event: GetAllDownloadsEvent) {
        event.respond(GetAllDownloadsEvent.ResultEvent(buildList {
            downloadManager.query(DownloadManager.Query()).use { cursor ->
                while (cursor.moveToNext()) {
                    add(Download(downloadManager, cursor))
                }
            }
        }))
    }

    @Subscribe
    fun onRemoveDownload(event: RemoveDownloadEvent) {
        event.respond(RemoveDownloadEvent.ResultEvent(downloadManager.remove(event.id) == 1))
    }

    /**
     * Adds download to the queue of the [DownloadManager].
     */
    private fun enqueueDownload(event: AddDownloadEvent, uri: Uri) {
        val fileName = event.fileName
            ?: URLUtil.guessFileName(event.uri, null, event.mimeType)

        val request = DownloadManager.Request(uri).apply {
            setDestinationInExternalPublicDir(SAVE_TO_DIR, fileName)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType(event.mimeType)

            event.headers?.forEach { addRequestHeader(it.first, it.second) }
        }

        if (Build.VERSION.SDK_INT >= 29) {
            event.tryRespond { AddDownloadEvent.ResultEvent(downloadManager.enqueue(request)) }
            return
        }

        activity.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
            event.tryRespond {
                check(granted) { "Permission required" }

                AddDownloadEvent.ResultEvent(downloadManager.enqueue(request))
            }
        }
    }

    /**
     * Writes data URI to a file and adds it as a completed download to [DownloadManager].
     */
    private fun addDownload(event: AddDownloadEvent, dataUri: DataUri) {
        val mimeType = dataUri.mimeType
            ?: event.mimeType
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(event.fileName?.substringAfterLast('.'))
            ?: FALLBACK_MIME_TYPE

        val fileName = event.fileName
            ?: FALLBACK_FILE_NAME.plus(MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: FALLBACK_EXTENSION)

        val saveToDir = Environment.getExternalStoragePublicDirectory(SAVE_TO_DIR)

        // Use MediaStore to write a file
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.RELATIVE_PATH, SAVE_TO_DIR)
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_DOWNLOAD, true)
            }

            val resolver = activity.contentResolver
            val contentUri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            val outputStream = requireNotNull(resolver.openOutputStream(contentUri))

            outputStream.use { it.write(dataUri.data) }

            val displayName =
                resolver.query(contentUri, arrayOf(MediaStore.Downloads.DISPLAY_NAME), null, null, null).use { cursor ->
                    requireNotNull(cursor).moveToFirst()
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME))
                }

            event.tryRespond {
                // Android 29 emulator throws java.lang.SecurityException: Unsupported path /storage/emulated/0/Download
                AddDownloadEvent.ResultEvent(addCompletedDownload(File(saveToDir, displayName), mimeType))
            }
            return
        }

        // Create a new file manually
        activity.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) { granted ->
            event.tryRespond {
                check(granted) { "Permission required" }

                val file = File(saveToDir, fileName).let {
                    // Prevent overwrite
                    if (it.exists()) File.createTempFile(it.nameWithoutExtension, it.extension, saveToDir) else it
                }
                file.writeBytes(dataUri.data)

                AddDownloadEvent.ResultEvent(addCompletedDownload(file, mimeType))
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun addCompletedDownload(file: File, mimeType: String) = downloadManager.addCompletedDownload(
        file.name,
        file.name,
        true,
        mimeType,
        file.absolutePath,
        file.length(),
        true
    )
}

private class DataUri(uri: Uri) {

    val mimeType: String?
    val data: ByteArray

    init {
        val parts = uri.schemeSpecificPart.split(';', ',').toMutableList()
        val encodedData = parts.removeLast()
        val isBase64 = parts.lastOrNull()?.trim().equals("base64", ignoreCase = true)

        data = if (isBase64) {
            parts.removeLast()
            Base64.getDecoder().decode(encodedData)
        } else {
            encodedData.toByteArray()
        }

        mimeType = parts.joinToString(";").ifBlank { null }
    }
}
