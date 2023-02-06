package org.racehorse

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Returns a new intent that is handled by any app that isn't among [excludedPackageNames] or is among
 * [includedPackageNames].
 */
fun Intent.constrainPackage(
    packageManager: PackageManager,
    excludedPackageNames: Array<String>? = null,
    includedPackageNames: Array<String>? = null,
): Intent? {

    if (excludedPackageNames == null && includedPackageNames == null) {
        return this
    }

    // Look for activities that that can handle this intent
    val packageNames = if (Build.VERSION.SDK_INT < 33) {
        packageManager.queryIntentActivities(this, 0)
    } else {
        packageManager.queryIntentActivities(this, PackageManager.ResolveInfoFlags.of(0L))
    }.map { it.activityInfo.packageName }

    val targetPackageNames = packageNames.filter { packageName ->
        val excluded = excludedPackageNames != null && excludedPackageNames.any { packageName.startsWith(it) }
        val included = includedPackageNames != null && includedPackageNames.any { packageName.startsWith(it) }

        !excluded || included
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
