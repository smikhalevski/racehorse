package org.racehorse

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import org.racehorse.webview.FileChooserCapability
import java.io.File
import java.io.IOException

/**
 * The plugin allows user to choose a file on the device.
 *
 * @param activity The activity that starts intents.
 * @param cacheDir The directory to store files captured by camera activity.
 * @param authority The [authority of the content provider](https://developer.android.com/guide/topics/providers/content-provider-basics#ContentURIs)
 * that provides access to [cacheDir] for camera app.
 */
open class FileChooserPlugin(
    val activity: ComponentActivity,
    val cacheDir: File? = null,
    val authority: String? = null
) : Plugin(), FileChooserCapability {

    companion object {
        fun parseResult(resultCode: Int, intent: Intent?): Array<Uri>? {
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

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        Request(filePathCallback, fileChooserParams).start()
        return true
    }

    private inner class Request(
        private val filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams,
    ) {

        private val multiple = fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE
        private val mimeType = fileChooserParams.acceptTypes.joinToString(",")

        private val anyRequested = mimeType == "" || mimeType.contains("*/*")
        private val imageRequested = mimeType.contains("image/")
        private val videoRequested = mimeType.contains("video/")

        fun start() {
            if (
                !imageRequested && !videoRequested ||
                !(cacheDir != null && authority != null && activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
            ) {
                // No camera-related MIME types, camera isn't supported, or capture result cannot be saved
                launchChooser(false)
                return
            }

            if (isPermissionGranted(activity, Manifest.permission.CAMERA)) {
                launchChooser(true)
                return
            }

            // Ask for camera permission
            activity.launchForActivityResult(
                ActivityResultContracts.RequestPermission(),
                Manifest.permission.CAMERA,
                this::launchChooser
            )
        }

        private fun launchChooser(cameraEnabled: Boolean) {
            // The shared file for camera capture
            var file: File? = null

            var intent = Intent(Intent.ACTION_GET_CONTENT)
                .setType(mimeType)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)

            if (cameraEnabled && cacheDir != null && authority != null) {
                try {
                    cacheDir.mkdirs()

                    file = File.createTempFile("camera", "", cacheDir)
                    file.deleteOnExit()

                    val fileUri = FileProvider.getUriForFile(activity, authority, file)

                    val imageIntent = if (anyRequested || imageRequested) {
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            .putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                    } else null

                    val videoIntent = if (anyRequested || videoRequested) {
                        Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            .putExtra(MediaStore.EXTRA_OUTPUT, fileUri)
                    } else null

                    intent = Intent.createChooser(intent, null)
                        .putExtra(
                            Intent.EXTRA_INITIAL_INTENTS,
                            arrayOf(imageIntent, videoIntent).filterNotNull().toTypedArray()
                        )

                } catch (exception: IOException) {
                    file?.delete()
                    file = null
                    exception.printStackTrace()
                }
            }

            val launched = activity.launchForActivityResult(ActivityResultContracts.StartActivityForResult(), intent) {
                val uris = parseResult(it.resultCode, it.data)

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
    }
}
