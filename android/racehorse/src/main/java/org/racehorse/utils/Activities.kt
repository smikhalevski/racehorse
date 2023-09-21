package org.racehorse.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.UUID

/**
 * Starts activity.
 *
 * @return `true` is activity was started, or `false` otherwise.
 */
fun Context.launchActivity(intent: Intent) = try {
    startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}

/**
 * Starts a one-time activity and returns its result via [callback].
 *
 * @param contract The contract specifying input/output types of the call.
 * @param input The input required to execute the [contract].
 * @param callback The callback that receives the result from the started activity.
 * @return `true` if activity has started, or `false` if there's no matching activity.
 */
fun <I, O> ComponentActivity.launchActivityForResult(
    contract: ActivityResultContract<I, O>,
    input: I,
    callback: ActivityResultCallback<O>
): Boolean {
    var launcher: ActivityResultLauncher<I>? = null

    launcher = activityResultRegistry.register(UUID.randomUUID().toString(), contract) {
        launcher?.unregister()
        callback.onActivityResult(it)
    }
    return try {
        launcher.launch(input)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/**
 * Starts a one-time activity and returns its result via [callback].
 *
 * @param intent The intent that starts an activity.
 * @param callback The callback that receives the result intent from the started activity.
 * @return `true` if activity has started, or `false` if there's no matching activity.
 */
fun ComponentActivity.launchActivityForResult(intent: Intent, callback: ActivityResultCallback<ActivityResult>) =
    launchActivityForResult(ActivityResultContracts.StartActivityForResult(), intent, callback)

/**
 * Shows a permission dialog for permissions that aren't granted yet.
 *
 * @param permissions The array of permissions that must be granted.
 * @param callback The callback the receives a map from a permission name to its granted status.
 */
fun ComponentActivity.askForPermissions(
    permissions: Iterable<String>,
    callback: (statuses: Map<String, Boolean>) -> Unit
) {
    val statuses = permissions.associateWith { true }
    val missingPermissions = permissions.filterNot(::isPermissionGranted).toTypedArray()

    if (missingPermissions.isEmpty()) {
        callback(statuses)
        return
    }

    launchActivityForResult(ActivityResultContracts.RequestMultiplePermissions(), missingPermissions) {
        callback(statuses + it)
    }
}

/**
 * Shows a permission dialog if a permission isn't granted yet.
 *
 * @param permission The permission to check.
 * @param callback The callback the receives `true` if the permission was granted, or `false` otherwise.
 */
fun ComponentActivity.askForPermission(permission: String, callback: (granted: Boolean) -> Unit) {
    if (isPermissionGranted(permission)) {
        callback(true)
    } else {
        launchActivityForResult(ActivityResultContracts.RequestPermission(), permission, callback)
    }
}

/**
 * Returns `true` if permission is granted, or `false` otherwise.
 *
 * @param permission The permission that must be checked.
 */
fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
