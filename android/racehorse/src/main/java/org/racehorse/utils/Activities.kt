package org.racehorse.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.util.UUID

/**
 * Launches a one-time activity and returns its result via [callback].
 *
 * @return `true` if activity has started, or `false` if there's no matching activity.
 */
fun <I, O> ComponentActivity.launchForActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
    callback: ActivityResultCallback<O>
): Boolean {
    var launcher: ActivityResultLauncher<I>? = null

    launcher = this.activityResultRegistry.register(UUID.randomUUID().toString(), contract) {
        launcher?.unregister()
        callback.onActivityResult(it)
    }
    return try {
        launcher.launch(input)
        true
    } catch (exception: ActivityNotFoundException) {
        false
    }
}

/**
 * Shows a permission dialog for permissions that aren't granted yet.
 *
 * @param permissions The array of permissions that must be granted.
 * @param callback The callback the receives a map from a permission name to its granted status.
 */
fun ComponentActivity.askForPermissions(
    permissions: Array<String>,
    callback: (statuses: Map<String, Boolean>) -> Unit
) {
    val statuses = permissions.associateWith { true }
    val missingPermissions = permissions.filterNot(::isPermissionGranted).toTypedArray()

    if (missingPermissions.isEmpty()) {
        callback(statuses)
        return
    }

    launchForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), missingPermissions) {
        callback(statuses + it)
    }
}

/**
 * Returns `true` if permission is granted, or `false` otherwise.
 *
 * @param permission The permission that must be checked.
 */
fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
