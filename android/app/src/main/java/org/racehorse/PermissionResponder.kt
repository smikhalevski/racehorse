package org.racehorse

import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.*

/**
 * Gets whether you should show UI with rationale before requesting a permission.
 */
class ShouldShowRequestPermissionRationaleRequestEvent(val permissions: Array<String>) : RequestEvent()

class ShouldShowTaskPermissionRationaleResponseEvent(val results: Map<String, Boolean>) : ResponseEvent()

/**
 * Determine whether you have been granted a particular permission.
 */
class IsPermissionGrantedRequestEvent(val permissions: Array<String>) : RequestEvent()

class IsPermissionGrantedResponseEvent(val results: Map<String, Boolean>) : ResponseEvent()

/**
 * Requests permissions to be granted to the application.
 */
class AskForPermissionRequestEvent(val permissions: Array<String>) : RequestEvent()

class AskForPermissionResponseEvent(val results: Map<String, Boolean>) : ResponseEvent()

/**
 * Responds to permission-related requests.
 */
class PermissionResponder(private val activity: ComponentActivity) {

    @Subscribe
    fun onShouldShowRequestPermissionRationaleRequestEvent(event: ShouldShowRequestPermissionRationaleRequestEvent) {
        event.postToChain(ShouldShowTaskPermissionRationaleResponseEvent(
            event.permissions.associateWith {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        ))
    }

    @Subscribe
    fun onIsPermissionGrantedRequestEvent(event: IsPermissionGrantedRequestEvent) {
        event.postToChain(IsPermissionGrantedResponseEvent(
            event.permissions.associateWith {
                ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
            }
        ))
    }

    @Subscribe
    fun onAskForPermissionRequestEvent(event: AskForPermissionRequestEvent) {
        val permissions = event.permissions

        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            event.postToChain(AskForPermissionResponseEvent(
                permissions.associateWith { true } + it
            ))
        }

        launcher.launch(
            permissions.filter {
                ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
            }.toTypedArray()
        )
    }
}
