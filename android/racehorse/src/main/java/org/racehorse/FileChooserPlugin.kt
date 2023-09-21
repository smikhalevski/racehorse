package org.racehorse

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.askForPermission
import org.racehorse.utils.launchActivityForResult
import org.racehorse.webview.ShowFileChooserEvent
import java.io.File
import java.io.IOException

/**
 * Allows the user to choose a file on the device.
 *
 * If [cacheDir] or [authority] are omitted, camera becomes unavailable since [FileChooserPlugin] cannot save
 * temporary files with captured images.
 *
 * @param activity The activity that starts intents.
 * @param cacheDir The directory to store files captured by camera activity.
 * @param authority The [authority of the content provider](https://developer.android.com/guide/topics/providers/content-provider-basics#ContentURIs)
 * that provides access to [cacheDir] for camera app.
 */
open class FileChooserPlugin(
    private val activity: ComponentActivity,
    private val cacheDir: File? = null,
    private val authority: String? = null
) {

    @Subscribe
    open fun onShowFileChooser(event: ShowFileChooserEvent) {
        if (event.shouldHandle()) {
            FileChooserLauncher(activity, cacheDir, authority, event.filePathCallback, event.fileChooserParams).start()
        }
    }
}

private class FileChooserLauncher(
    val activity: ComponentActivity,
    val cacheDir: File?,
    val authority: String?,
    val filePathCallback: ValueCallback<Array<Uri>>,
    val fileChooserParams: FileChooserParams,
) {

    private val mimeTypes = fileChooserParams.acceptTypes.joinToString(",")

    private val isImage = mimeTypes.isEmpty() || "*/*" in mimeTypes || "image/" in mimeTypes
    private val isVideo = mimeTypes.isEmpty() || "*/*" in mimeTypes || "video/" in mimeTypes

    fun start() {
        if (
            (isImage || isVideo) &&
            (cacheDir != null && authority != null && activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
        ) {
            activity.askForPermission(Manifest.permission.CAMERA, ::launchChooser)
        } else {
            // No camera-related MIME types, camera isn't supported, or capture result cannot be saved
            launchChooser(false)
        }
    }

    private fun launchChooser(isCameraEnabled: Boolean) {
        // The shared file for camera capture
        var file: File? = null

        val fileUri = if (isCameraEnabled && cacheDir != null && authority != null) {
            try {
                cacheDir.mkdirs()

                file = File.createTempFile("camera", "", cacheDir)
                file.deleteOnExit()

                FileProvider.getUriForFile(activity, authority, file)
            } catch (e: IOException) {
                file?.delete()
                file = null
                e.printStackTrace()
                null
            }
        } else null

        var intent = Intent(Intent.ACTION_GET_CONTENT)
            .setType(mimeTypes)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)

        if (fileUri != null) {
            val extraIntents = ArrayList<Intent>()

            if (isImage) {
                extraIntents.add(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                )
            }
            if (isVideo) {
                extraIntents.add(
                    Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                        .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                        .putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                )
            }
            if (extraIntents.isNotEmpty()) {
                intent = Intent.createChooser(intent, null)
                    .putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents.toTypedArray())
            }
        }

        val launched = activity.launchActivityForResult(intent) {
            val uris = parseFileChooserResult(it.resultCode, it.data)

            filePathCallback.onReceiveValue(
                when {
                    uris != null -> uris

                    // Maybe user captured an image or video with a camera
                    file != null -> if (file.length() == 0L) {
                        file.delete()
                        arrayOf()
                    } else {
                        arrayOf(Uri.fromFile(file))
                    }

                    else -> arrayOf()
                }
            )
        }

        if (!launched) {
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
