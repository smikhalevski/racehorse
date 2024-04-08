package org.racehorse

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.askForPermission
import org.racehorse.utils.copyTo
import org.racehorse.utils.guessMimeTypeFromContent
import org.racehorse.utils.launchActivityForResult
import org.racehorse.webview.ShowFileChooserEvent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Allows the user to choose a file on the device.
 *
 * @param activity The activity that starts intents.
 */
open class FileChooserPlugin(
    private val activity: ComponentActivity,
    private val cameraFileFactory: CameraFileFactory? = null
) {

    @Subscribe
    open fun onShowFileChooser(event: ShowFileChooserEvent) {
        if (event.shouldHandle()) {
            FileChooserLauncher(activity, cameraFileFactory, event.filePathCallback, event.fileChooserParams).start()
        }
    }
}

interface CameraFileFactory {
    /**
     * Creates a new camera file, or returns `null` if file cannot be created.
     */
    fun create(callback: (cameraFile: CameraFile?) -> Unit)
}

interface CameraFile {
    /**
     * The URI that camera app uses to store the captured data.
     */
    val cameraContentUri: Uri

    /**
     * Returns the persisted file URI that is returned by the file chooser to web view, or `null` if file cannot be
     * returned from file chooser (for example, if file is empty, or non-existent).
     */
    fun resolveFileChooserUri(): Uri?
}

/**
 * File provider that stores camera files in a cache directory.
 *
 * @param context A context used by [FileProvider].
 * @param cacheDir The directory to store files captured by the camera app.
 * @param cacheDirAuthority The authority of a [FileProvider] defined in a `<provider>` element in your app's manifest.
 */
class CachedCameraFileFactory(
    private val context: Context,
    private val cacheDir: File,
    private val cacheDirAuthority: String
) : CameraFileFactory {

    override fun create(callback: (cameraFile: CameraFile?) -> Unit) {
        val file = createNewFile(cacheDir, DEFAULT_PREFIX).apply { deleteOnExit() }

        callback(CachedCameraFile(file, FileProvider.getUriForFile(context, cacheDirAuthority, file)))
    }
}

private class CachedCameraFile(private val file: File, override val cameraContentUri: Uri) : CameraFile {

    override fun resolveFileChooserUri(): Uri? {
        if (file.length() == 0L) {
            file.delete()
            return null
        }

        val mimeType = runCatching(file::guessMimeTypeFromContent).getOrNull()

        return mimeType
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
            ?.let { createNewFile(checkNotNull(file.parentFile), getPrefixForMimeType(mimeType), ".$it") }
            ?.takeIf(file::renameTo)
            ?.also(File::deleteOnExit)
            ?.toUri()
            ?: file.toUri()
    }
}

class GalleryCameraFileFactory(
    private val activity: ComponentActivity,
    private val tempDir: File,
    private val tempDirAuthority: String
) : CameraFileFactory {

    override fun create(callback: (cameraFile: CameraFile?) -> Unit) {
        activity.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) { isGranted ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || isGranted) {
                val tempFile = createNewFile(tempDir, DEFAULT_PREFIX).apply { deleteOnExit() }

                callback(GalleryCameraFile(activity, tempFile, FileProvider.getUriForFile(activity, tempDirAuthority, tempFile)))
            } else {
                callback(null)
            }
        }
    }
}

private class GalleryCameraFile(
    private val activity: ComponentActivity,
    private val tempFile: File,
    override val cameraContentUri: Uri
) : CameraFile {

    companion object {
        private const val DEFAULT_MIME_TYPE = "application/octet-stream"
        private const val DEFAULT_EXTENSION = "bin"
    }

    override fun resolveFileChooserUri(): Uri? {
        if (tempFile.length() == 0L) {
            tempFile.delete()
            return null
        }

        val mimeType = runCatching(tempFile::guessMimeTypeFromContent).getOrNull() ?: DEFAULT_MIME_TYPE
        val prefix = getPrefixForMimeType(mimeType)
        val suffix = ".${MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: DEFAULT_EXTENSION}"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, getFileName(prefix, suffix))
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            values.put(MediaStore.MediaColumns.IS_PENDING, true)

            val storageUri = if ("video/" in mimeType) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentUri = checkNotNull(activity.contentResolver.insert(storageUri, values))

            try {
                checkNotNull(activity.contentResolver.openOutputStream(contentUri)).use(tempFile::copyTo)

                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, false)
                activity.contentResolver.update(contentUri, values, null, null)

                return contentUri
            } catch (e: Throwable) {
                activity.contentResolver.delete(contentUri, null, null)
                throw e
            } finally {
                tempFile.delete()
            }
        }

        val targetDir = if ("video/" in mimeType) Environment.DIRECTORY_MOVIES  else Environment.DIRECTORY_PICTURES
        val targetFile = createNewFile(Environment.getExternalStoragePublicDirectory(targetDir), prefix, suffix)

        try {
            tempFile.copyTo(targetFile)
            return targetFile.toUri()
        } finally {
            tempFile.delete()
        }
    }
}

private class FileChooserLauncher(
    private val activity: ComponentActivity,
    private val cameraFileFactory: CameraFileFactory?,
    private val filePathCallback: ValueCallback<Array<Uri>>,
    private val fileChooserParams: FileChooserParams,
) {

    private val mimeTypes = fileChooserParams.acceptTypes.filter { it.isNotEmpty() }

    private val isImage = mimeTypes.any { "*/*" in it || "image/" in it }
    private val isVideo = mimeTypes.any { "*/*" in it || "video/" in it }

    fun start() {
        if (
            cameraFileFactory == null ||
            !(isImage || isVideo) ||
            !activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        ) {
            // No camera-related MIME types, camera isn't supported, or capture result cannot be saved
            launchChooser(null)
        }

        activity.askForPermission(Manifest.permission.CAMERA) { isGranted ->
            if (isGranted) {
                checkNotNull(cameraFileFactory).create(::launchChooser)
            } else {
                launchChooser(null)
            }
        }
    }

    private fun launchChooser(cameraFile: CameraFile?) {
        var intent = Intent(Intent.ACTION_GET_CONTENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)

        if (mimeTypes.isNotEmpty()) {
            if (mimeTypes.size == 1) {
                intent.setTypeAndNormalize(mimeTypes[0])
            } else {
                intent.setType("*/*").putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
            }
        }

        if (cameraFile != null) {
            val cameraIntents = ArrayList<Intent>()

            if (isImage) {
                cameraIntents.add(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra(MediaStore.EXTRA_OUTPUT, cameraFile.cameraContentUri)
                )
            }
            if (isVideo) {
                cameraIntents.add(
                    Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra(MediaStore.EXTRA_OUTPUT, cameraFile.cameraContentUri)
                )
            }
            if (cameraIntents.isNotEmpty()) {
                intent = Intent.createChooser(intent, null)
                    .putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toTypedArray())
            }
        }

        val isLaunched = activity.launchActivityForResult(intent) { result ->
            filePathCallback.onReceiveValue(
                try {
                    cameraFile
                        ?.resolveFileChooserUri()
                        ?.let { arrayOf(it) }
                        ?: parseFileChooserResult(result.resultCode, result.data)
                        ?: arrayOf()
                } catch (e: Throwable) {
                    e.printStackTrace()
                    arrayOf()
                }
            )
        }

        if (!isLaunched) {
            // No activity that can provide files
            filePathCallback.onReceiveValue(arrayOf())
        }
    }

    private fun parseFileChooserResult(resultCode: Int, intent: Intent?): Array<Uri>? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        intent?.data?.let {
            return arrayOf(it)
        }
        intent?.clipData?.let { clipData ->
            return (0 until clipData.itemCount).mapNotNull { clipData.getItemAt(it).uri }.toTypedArray()
        }
        return null
    }
}

private const val DEFAULT_PREFIX = "CAMERA"

private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

private fun getPrefixForMimeType(mimeType: String) = if ("video/" in mimeType) "VID" else "IMG"

private fun getFileName(prefix: String, suffix: String) = "${prefix}_${dateFormat.format(Date())}${suffix}"

private fun createNewFile(targetDir: File, prefix: String, suffix: String = ""): File {
    targetDir.mkdirs()

    return File(targetDir, getFileName(prefix, suffix)).takeIf(File::createNewFile)
        ?: File.createTempFile(getFileName(prefix, "_"), suffix, targetDir)
}
