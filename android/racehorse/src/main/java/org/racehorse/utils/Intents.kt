package org.racehorse.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Returns the new intent that is applied only to packages that passed the filter.
 *
 * For example, to prevent sharing a URL via Bluetooth:
 *
 * ```kotlin
 * val intent = Intent(Intent.ACTION_SEND)
 *     .setType("text/plain")
 *     .putExtra(Intent.EXTRA_TEXT, "http://example.com")
 *     .filterPackageNames(activity.packageManager) { "bluetooth" !in it }
 * ```
 *
 * Add query to the manifest:
 *
 * ```xml
 * <manifest>
 *   <queries>
 *     <intent>
 *       <action android:name="android.intent.action.SEND" />
 *       <data android:mimeType="text/plain" />
 *     </intent>
 *   </queries>
 * </manifest>
 * ```
 */
fun Intent.filterPackageNames(packageManager: PackageManager, predicate: (packageName: String) -> Boolean): Intent? {
    val resolveInfos = if (Build.VERSION.SDK_INT >= 33) {
        packageManager.queryIntentActivities(this, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        packageManager.queryIntentActivities(this, 0)
    }

    val intents = resolveInfos.mapNotNull {
        it.activityInfo.packageName.takeIf(predicate)?.let(Intent(this)::setPackage)
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
