package org.racehorse.utils

import android.content.ActivityNotFoundException
import android.view.View
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import java.util.*

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
