package org.racehorse.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Returns the new intent that wouldn't be applied to the apps with given package names.
 *
 * Requires [`QUERY_ALL_PACKAGES`](https://developer.android.com/training/package-visibility) permission, otherwise
 * no activity would be started.
 */
fun Intent.excludePackages(packageManager: PackageManager, excludedPackageNames: Array<String>): Intent? {
    if (excludedPackageNames.isEmpty()) {
        return this
    }

    val resolveInfos = if (Build.VERSION.SDK_INT >= 33) {
        packageManager.queryIntentActivities(this, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(this, 0)
    }

    val intents = resolveInfos.mapNotNull { resolveInfo ->
        resolveInfo.activityInfo.packageName
            ?.takeIf { excludedPackageNames.none(it::startsWith) }
            ?.let { Intent(this).setPackage(it) }
    }

    return if (intents.size > 1) {
        Intent.createChooser(intents[0], null).putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(1).toTypedArray())
    } else {
        intents.firstOrNull()
    }
}

/**
 * Returns the action name that is most suitable for the URI.
 */
fun Uri.guessIntentAction() = when (scheme) {
    // https://developer.android.com/guide/components/intents-common#Phone
    "voicemail", "tel" -> Intent.ACTION_DIAL

    // https://developer.android.com/guide/components/intents-common#Messaging
    // https://developer.android.com/guide/components/intents-common#Email
    "sms", "smsto", "mms", "mmsto", "mailto" -> Intent.ACTION_SENDTO

    else -> Intent.ACTION_VIEW
}
