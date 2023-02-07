package org.racehorse

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

fun Intent.getActivityPackageNames(packageManager: PackageManager): List<String> {
    return if (Build.VERSION.SDK_INT < 33) {
        packageManager.queryIntentActivities(this, 0)
    } else {
        packageManager.queryIntentActivities(this, PackageManager.ResolveInfoFlags.of(0L))
    }.map { it.activityInfo.packageName }
}

fun Intent.excludePackage(packageManager: PackageManager, excludedPackageNames: Array<String>): Intent? {
    val packageNames = getActivityPackageNames(packageManager)

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

//fun createChooser(packageManager: PackageManager, intents: Array<Intent>): Intent? {
//    val extraIntents = ArrayList<Intent>()
//
//    for (intent in intents) {
//        for (packageName in intent.getActivityPackageNames(packageManager)) {
//            if (extraIntents.none { it.getPackage() == packageName && it.action == intent.action }) {
//                extraIntents.add(Intent(intent).setPackage(packageName))
//            }
//        }
//    }
//    if (extraIntents.isEmpty()) {
//        return null
//    }
//    if (extraIntents.size == 1) {
//        return extraIntents[0]
//    }
//    return Intent.createChooser(intents[0], null).putExtra(Intent.EXTRA_INITIAL_INTENTS)
//}
