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
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.core.database.getStringOrNull
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.askForPermission
import org.racehorse.utils.createTempFile
import org.racehorse.utils.queryAll
import org.racehorse.webview.DownloadStartEvent
import java.io.File
import java.util.Base64

@Serializable
class Download(
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
     * Otherwise, the value will initially be `null` and will be filled in with a generated URI once the download has
     * started.
     */
    val localUri: String?,

    /**
     * The `content:` URI of the downloaded file.
     */
    val contentUri: @Contextual Uri?,

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
     * A timestamp when the download was last modified (wall clock time in UTC).
     */
    val lastModifiedTime: Int,

    /**
     * The client-supplied title for this download.
     */
    val title: String
) {
    constructor(downloadManager: DownloadManager, cursor: Cursor) : this(
        id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)),
        status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
        reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)),
        uri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)),
        localUri = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)),
        contentUri = downloadManager.getUriForDownloadedFile(cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))),
        mimeType = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)),
        totalSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
        downloadedSize = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
        lastModifiedTime = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP)),
        title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)),
    )
}

/**
 * Adds a new download.
 */
@Serializable
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
    val headers: List<List<String>>? = null,
) : RequestEvent() {

    @Serializable
    class ResultEvent(val id: Long) : ResponseEvent()
}

/**
 * Returns a previously started file download by its ID.
 */
@Serializable
class GetDownloadEvent(var id: Long) : RequestEvent() {

    @Serializable
    class ResultEvent(val download: Download?) : ResponseEvent()
}

/**
 * Returns all file downloads.
 */
@Serializable
class GetAllDownloadsEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val downloads: List<Download>) : ResponseEvent()
}

/**
 * Cancel a download and remove it from the download manager. Download will be stopped if it was running, and it will no
 * longer be accessible through the download manager. If there is a downloaded file, partial or complete, it is deleted.
 */
@Serializable
class RemoveDownloadEvent(var id: Long) : RequestEvent() {

    @Serializable
    class ResultEvent(val isRemoved: Boolean) : ResponseEvent()
}

open class DownloadPlugin(private val activity: ComponentActivity) {

    private companion object {
        val TARGET_DIR = Environment.DIRECTORY_DOWNLOADS

        const val DEFAULT_MIME_TYPE = "application/octet-stream"
        const val DEFAULT_FILE_NAME = "data"
        const val DEFAULT_EXTENSION = "bin"
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

            else -> event.respond(ExceptionEvent(IllegalArgumentException("Unsupported URI")))
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

        val request = DownloadManager.Request(uri)
            .setDestinationInExternalPublicDir(TARGET_DIR, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType(event.mimeType)

        event.headers?.forEach { (key, value) -> request.addRequestHeader(key, value) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            event.respond(AddDownloadEvent.ResultEvent(downloadManager.enqueue(request)))
            return
        }

        activity.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) { isGranted ->
            event.respond {
                isGranted || return@respond ExceptionEvent(PermissionRequiredException())

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
            ?: DEFAULT_MIME_TYPE

        val fileName = event.fileName
            ?: "$DEFAULT_FILE_NAME.${
                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: DEFAULT_EXTENSION
            }"

        // Use MediaStore to write a file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, TARGET_DIR)
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            values.put(MediaStore.MediaColumns.IS_DOWNLOAD, true)
            values.put(MediaStore.MediaColumns.IS_PENDING, true)

            val contentUri =
                checkNotNull(activity.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            try {
                checkNotNull(activity.contentResolver.openOutputStream(contentUri)).use { it.write(dataUri.data) }

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, false)
                activity.contentResolver.update(contentUri, values, null, null)
            } catch (e: Throwable) {
                activity.contentResolver.delete(contentUri, null, null)
                throw e
            }

            val filePath = activity.contentResolver.queryAll(contentUri, arrayOf(MediaStore.Downloads.DATA)) {
                moveToFirst()
                getString(getColumnIndexOrThrow(MediaStore.Downloads.DATA))
            }

            activity.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                event.respond { AddDownloadEvent.ResultEvent(addCompletedDownload(File(filePath), mimeType)) }
            }
            return
        }

        // Create a new file manually
        activity.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) { isGranted ->
            event.respond {
                isGranted || return@respond ExceptionEvent(PermissionRequiredException())

                val targetDir = Environment.getExternalStoragePublicDirectory(TARGET_DIR)

                val file = File(targetDir, fileName).let { if (it.createNewFile()) it else it.createTempFile() }
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

@SuppressLint("NewApi")
private class DataUri(uri: Uri) {

    val mimeType: String?
    val data: ByteArray

    init {
        val parts = uri.schemeSpecificPart.split(';', ',').toMutableList()
        val encodedData = parts.removeAt(parts.lastIndex)
        val isBase64 = parts.lastOrNull()?.trim().equals("base64", ignoreCase = true)

        data = if (isBase64) {
            parts.removeAt(parts.lastIndex)
            Base64.getDecoder().decode(encodedData)
        } else {
            encodedData.toByteArray()
        }

        mimeType = parts.joinToString(";").ifBlank { null }
    }
}
