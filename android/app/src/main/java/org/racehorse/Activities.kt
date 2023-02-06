package org.racehorse

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import java.util.*

/**
 * Launches a one-time activity and returns its result via [callback].
 */
fun <I, O> ComponentActivity.launchForActivityResult(
    contract: ActivityResultContract<I, O>,
    input: I,
    callback: ActivityResultCallback<O>
) {
    var launcher: ActivityResultLauncher<I>? = null

    launcher = this.activityResultRegistry.register(UUID.randomUUID().toString(), contract) {
        launcher?.unregister()
        callback.onActivityResult(it)
    }
    launcher.launch(input)
}
