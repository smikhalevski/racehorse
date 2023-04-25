package org.racehorse.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.Serializable
import java.util.*

class WebActivityResult<T>(val resultCode: Int, val data: T)

class WebIntent(
    /**
     * The general action to be performed, such as [Intent.ACTION_VIEW]. The action describes the general way
     * the rest of the information in the intent should be interpreted — most importantly, what to do with the [uri].
     */
    val action: String? = null,

    /**
     * The data this intent is operating on. This URI specifies the name of the data; often it uses the content: scheme,
     * specifying data in a content provider. Other schemes may be handled by specific activities, such as http: by the
     * web browser.
     */
    val uri: String? = null,

    /**
     * Any special flags associated with this intent, such as [Intent.FLAG_ACTIVITY_NEW_TASK].
     */
    val flags: Int = 0,

    /**
     * A map of extended data from the intent.
     */
    val extras: Map<String, Serializable?>? = null,
)

/**
 * Launches a one-time activity and returns its result via [callback].
 *
 * @return `true` if activity has started, or `false` if there's no matching activity.
 */
fun <I, O> ComponentActivity.startActivityForResult(
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
    } catch (_: ActivityNotFoundException) {
        false
    }
}

fun ComponentActivity.startActivityForResult(intent: Intent, callback: ActivityResultCallback<ActivityResult>) =
    startActivityForResult(ActivityResultContracts.StartActivityForResult(), intent, callback)

fun ActivityResult.toWebActivityResult() = WebActivityResult(resultCode, data?.toWebIntent())

fun Intent.toWebIntent() = WebIntent(
    action,
    dataString,
    flags,
    extras?.keySet()?.associateWith(::getSerializableExtra)
)

fun WebIntent.toIntent(): Intent {
    val intent = Intent(action)

    intent.data = uri?.let(Uri::parse)
    intent.flags = flags

    extras?.forEach(intent::putExtra)

    return intent
}

/**
 * Returns the new intent that wouldn't be applied to the apps with given package names.
 */
fun Intent.excludePackages(packageManager: PackageManager, excludedPackageNames: Array<String>): Intent? {

    // Package names of activities that support this intent
    val activityPackageNames = if (Build.VERSION.SDK_INT >= 33) {
        packageManager.queryIntentActivities(this, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(this, 0)
    }.map { it.activityInfo.packageName }

    val packageNames = activityPackageNames.filter { packageName ->
        excludedPackageNames.none { packageName.startsWith(it) }
    }

    if (packageNames.isEmpty()) {
        return null
    }

    val intent = Intent(this).setPackage(packageNames[0])

    return if (packageNames.size == 1) intent else {

        // There are several suitable apps, let the user decide which one to start
        Intent.createChooser(intent, null).putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            packageNames.drop(1).map { Intent(this).setPackage(it) }.toTypedArray()
        )
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

    startActivityForResult(ActivityResultContracts.RequestMultiplePermissions(), missingPermissions) {
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
