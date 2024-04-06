package org.racehorse

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.askForPermission
import org.racehorse.utils.getMimeTypeFromSignature
import org.racehorse.utils.launchActivityForResult
import org.racehorse.webview.ShowFileChooserEvent
import java.io.File
import java.io.IOException

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
    fun create(fileName: String?, callback: (cameraFile: CameraFile?) -> Unit)
}

interface CameraFile {
    /**
     * File URI that is shared with the camera app that flushes captured data to it.
     */
    val contentUri: Uri

    /**
     * Returns the persisted file URI that is returned by the file chooser to web view, or `null` if file cannot be
     * returned from file chooser (for example, if file is empty, or non-existent).
     */
    fun retrieveFileChooserUri(): Uri?
}

class TempCameraFileFactory(
    private val context: Context,
    private val cacheDir: File,
    private val authority: String
) : CameraFileFactory {

    override fun create(fileName: String?, callback: (cameraFile: CameraFile?) -> Unit) {
        var file: File? = null

        try {
            cacheDir.mkdirs()

            file = File.createTempFile("camera", "", cacheDir)
            file.deleteOnExit()

            callback(TempCameraFile(file, FileProvider.getUriForFile(context, authority, file)))
        } catch (e: IOException) {
            file?.delete()
            e.printStackTrace()
            callback(null)
        }
    }
}

private class TempCameraFile(private val file: File, override val contentUri: Uri) : CameraFile {

    override fun retrieveFileChooserUri(): Uri? {
        if (file.length() == 0L) {
            file.delete()
            return null
        }

        return Uri.fromFile(
            file.getMimeTypeFromSignature()
                ?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
                ?.let { File("${file.absolutePath}.$it") }
                ?.takeIf(file::renameTo)
                ?.also(File::deleteOnExit)
                ?: file
        )
    }
}

private class FileChooserLauncher(
    private val activity: ComponentActivity,
    private val cameraFileFactory: CameraFileFactory?,
    private val filePathCallback: ValueCallback<Array<Uri>>,
    private val fileChooserParams: FileChooserParams,
) {

    private val mimeTypes = fileChooserParams.acceptTypes.joinToString(",")

    private val isImage = mimeTypes.isEmpty() || "*/*" in mimeTypes || "image/" in mimeTypes
    private val isVideo = mimeTypes.isEmpty() || "*/*" in mimeTypes || "video/" in mimeTypes

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
                requireNotNull(cameraFileFactory).create(fileChooserParams.filenameHint, ::launchChooser)
            } else {
                launchChooser(null)
            }
        }
    }

    private fun launchChooser(cameraFile: CameraFile?) {
        var intent = Intent(Intent.ACTION_GET_CONTENT)
            .setType(mimeTypes)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)

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
                    cameraFile?.retrieveFileChooserUri()?.let { arrayOf(it) }
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
