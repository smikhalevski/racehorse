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
import org.racehorse.utils.guessMimeType
import org.racehorse.utils.launchActivityForResult
import org.racehorse.webview.ShowFileChooserEvent
import java.io.File

/**
 * Allows the user to choose a file on the device.
 *
 * @param activity The activity that starts intents.
 * @param cameraFileFactory Crates a file that camera app uses to store the captured data.
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
     * The URI that camera app should use to store the captured data.
     */
    val contentUri: Uri

    /**
     * Returns the persisted file URI that is returned by the file chooser to web view, or `null` if file cannot be
     * returned from file chooser (for example, if file is empty, or non-existent).
     */
    fun getOutputUri(): Uri?
}

private const val CAMERA_FILE_PREFIX = "camera"

/**
 * Camera file factory that stores camera files in a temporary cache.
 *
 * @param context A context used by [FileProvider].
 * @param tempDir The directory to store files captured by the camera app.
 * @param tempDirAuthority The authority of a [FileProvider] defined in a `<provider>` element in your app's manifest.
 */
class TempCameraFileFactory(
    private val context: Context,
    private val tempDir: File,
    private val tempDirAuthority: String
) : CameraFileFactory {

    override fun create(callback: (cameraFile: CameraFile?) -> Unit) {
        val tempFile = File.createTempFile(CAMERA_FILE_PREFIX, "", tempDir)
        tempFile.deleteOnExit()

        callback(TempCameraFile(FileProvider.getUriForFile(context, tempDirAuthority, tempFile), tempFile))
    }
}

private class TempCameraFile(override val contentUri: Uri, private val tempFile: File) : CameraFile {

    override fun getOutputUri(): Uri? {
        if (tempFile.length() == 0L) {
            tempFile.delete()
            return null
        }

        return tempFile.guessMimeType()
            ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
            ?.let { File(tempFile.absolutePath + ".$it") }
            ?.takeIf(tempFile::renameTo)
            ?.apply(File::deleteOnExit)
            ?.toUri()
            ?: tempFile.toUri()
    }
}

/**
 * Camera file factory that stores camera files in picture gallery.
 *
 * @param activity An activity used by [FileProvider].
 * @param tempDir The directory to store files captured by the camera app.
 * @param tempDirAuthority The authority of a [FileProvider] defined in a `<provider>` element in your app's manifest.
 */
class GalleryCameraFileFactory(
    private val activity: ComponentActivity,
    private val tempDir: File,
    private val tempDirAuthority: String
) : CameraFileFactory {

    override fun create(callback: (cameraFile: CameraFile?) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callback(createCameraFile())
            return
        }

        activity.askForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) { isGranted ->
            callback(if (isGranted) createCameraFile() else null)
        }
    }

    private fun createCameraFile(): CameraFile {
        val tempFile = File.createTempFile(CAMERA_FILE_PREFIX, "", tempDir)
        tempFile.deleteOnExit()

        return GalleryCameraFile(FileProvider.getUriForFile(activity, tempDirAuthority, tempFile), activity, tempFile)
    }
}

private class GalleryCameraFile(
    override val contentUri: Uri,
    private val activity: ComponentActivity,
    private val tempFile: File,
) : CameraFile {

    private companion object {
        const val DEFAULT_MIME_TYPE = "application/octet-stream"
        const val DEFAULT_EXTENSION = "bin"
    }

    override fun getOutputUri(): Uri? {
        if (tempFile.length() == 0L) {
            tempFile.delete()
            return null
        }

        val mimeType = tempFile.guessMimeType() ?: DEFAULT_MIME_TYPE
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: DEFAULT_EXTENSION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "$CAMERA_FILE_PREFIX.$extension")
            values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            values.put(MediaStore.MediaColumns.IS_PENDING, true)

            val storageUri =
                if ("video/" in mimeType) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
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

        val storageDir = Environment.getExternalStoragePublicDirectory(
            if ("video/" in mimeType) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
        )
        val file = File.createTempFile(CAMERA_FILE_PREFIX, ".$extension", storageDir)

        try {
            tempFile.copyTo(file.outputStream())
            return file.toUri()
        } catch (e: Throwable) {
            file.delete()
            throw e
        } finally {
            tempFile.delete()
        }
    }
}

private class FileChooserLauncher(
    private val activity: ComponentActivity,
    private val cameraFileFactory: CameraFileFactory?,
    private val filePathCallback: ValueCallback<Array<Uri>?>,
    private val fileChooserParams: FileChooserParams,
) {

    private val mimeTypes = fileChooserParams.acceptTypes.filter { it.isNotBlank() }

    private val isImage = mimeTypes.any { it.startsWith("*/*") || it.startsWith("image/") }
    private val isVideo = mimeTypes.any { it.startsWith("*/*") || it.startsWith("video/") }

    fun start() {
        if (
            cameraFileFactory == null ||
            !(isImage || isVideo) ||
            !activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        ) {
            // No camera-related MIME types, camera isn't supported, or capture result cannot be saved
            launchChooser(null)
            return
        }

        activity.askForPermission(Manifest.permission.CAMERA) { isGranted ->
            if (isGranted) {
                cameraFileFactory.create(::launchChooser)
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
                        .putExtra(MediaStore.EXTRA_OUTPUT, cameraFile.contentUri)
                )
            }
            if (isVideo) {
                cameraIntents.add(
                    Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra(MediaStore.EXTRA_OUTPUT, cameraFile.contentUri)
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
                        ?.getOutputUri()
                        ?.let { arrayOf(it) }
                        ?: parseFileChooserResult(result.resultCode, result.data)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }
            )
        }

        if (!isLaunched) {
            // No activity that can provide files
            filePathCallback.onReceiveValue(null)
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
