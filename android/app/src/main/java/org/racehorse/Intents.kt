package org.racehorse

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

fun Intent.excludePackage(packageManager: PackageManager, excludedPackageNames: Array<String>): Intent? {

    // Look for activities that that can handle this intent
    val packageNames = if (Build.VERSION.SDK_INT < 33) {
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

    // There are several suitable activities, let the user decide which one to start
    return Intent.createChooser(this, null).putExtra(
        Intent.EXTRA_INITIAL_INTENTS,
        targetPackageNames.map { Intent(this).setPackage(it) }.toTypedArray()
    )
}
