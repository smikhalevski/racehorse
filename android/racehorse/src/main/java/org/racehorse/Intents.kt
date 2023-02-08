package org.racehorse

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Returns the new intent that wouldn't be applied to the apps with given package names.
 *
 * **Note:** `<queries>` must be present in the manifest file.
 */
fun Intent.excludePackage(packageManager: PackageManager, excludedPackageNames: Array<String>): Intent? {
    val packageNames = if (Build.VERSION.SDK_INT < 33) {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(this, 0)
    } else {
        packageManager.queryIntentActivities(this, PackageManager.ResolveInfoFlags.of(0L))
    }.map { it.activityInfo.packageName }

    val targetPackageNames = packageNames.filter { packageName ->
        excludedPackageNames.none { packageName.startsWith(it) }
    }

    if (targetPackageNames.isEmpty()) {
        return null
    }
    if (targetPackageNames.size == 1) {
        return Intent(this).setPackage(targetPackageNames[0])
    }

    // There are several suitable apps, let the user decide which one to start
    return Intent
        .createChooser(Intent(this).setPackage(targetPackageNames[0]), null)
        .putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            targetPackageNames.drop(1).map { Intent(this).setPackage(it) }.toTypedArray()
        )
}
