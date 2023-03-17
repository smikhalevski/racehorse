package org.racehorse.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Returns the new intent that wouldn't be applied to the apps with given package names.
 */
fun Intent.excludePackage(packageManager: PackageManager, excludedPackageNames: Array<String>): Intent? {

    // Package names of activities that support this intent
    val activityPackageNames = if (Build.VERSION.SDK_INT < 33) {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(this, 0)
    } else {
        packageManager.queryIntentActivities(this, PackageManager.ResolveInfoFlags.of(0L))
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
