package org.racehorse.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.result.ActivityResult
import java.io.Serializable

class WebActivityResult<T>(val resultCode: Int, val data: T)

/**
 * @param action The general action to be performed, such as [Intent.ACTION_VIEW].
 * @param data The URI-encoded data that intent is operating on.
 * @param flags Special flags associated with this intent, such as [Intent.FLAG_ACTIVITY_NEW_TASK].
 * @param extras A map of extended data from the intent.
 */
class WebIntent(val action: String?, val data: String?, val flags: Int, val extras: Map<String, Serializable?>?)

fun ActivityResult.toWebActivityResult() = WebActivityResult(resultCode, data?.toWebIntent())

fun Intent.toWebIntent() = WebIntent(
    action,
    dataString,
    flags,
    extras?.keySet()?.associateWith(::getSerializableExtra)
)

fun WebIntent.toIntent(): Intent {
    val intent = Intent(action)

    intent.data = data?.let(Uri::parse)
    intent.flags = flags

    extras?.forEach(intent::putExtra)

    return intent
}

/**
 * Returns the new intent that wouldn't be applied to the apps with given package names. Requires the
 * `android.permission.QUERY_ALL_PACKAGES` permission, otherwise no activity would be started.
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
