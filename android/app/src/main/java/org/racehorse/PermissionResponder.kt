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

class ShouldShowTaskPermissionRationaleResponseEvent(val statuses: Map<String, Boolean>) : ResponseEvent()

/**
 * Determine whether you have been granted a particular permission.
 */
class IsPermissionGrantedRequestEvent(val permissions: Array<String>) : RequestEvent()

class IsPermissionGrantedResponseEvent(val statuses: Map<String, Boolean>) : ResponseEvent()

/**
 * Requests permissions to be granted to the application.
 */
class AskForPermissionRequestEvent(val permissions: Array<String>) : RequestEvent()

class AskForPermissionResponseEvent(val statuses: Map<String, Boolean>) : ResponseEvent()

/**
 * Responds to permission-related requests.
 */
class PermissionResponder(private val activity: ComponentActivity) {

    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    @Subscribe
    fun onShouldShowRequestPermissionRationaleRequestEvent(event: ShouldShowRequestPermissionRationaleRequestEvent) {
        event.respond(ShouldShowTaskPermissionRationaleResponseEvent(
            event.permissions.associateWith {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        ))
    }

    @Subscribe
    fun onIsPermissionGrantedRequestEvent(event: IsPermissionGrantedRequestEvent) {
        event.respond(IsPermissionGrantedResponseEvent(event.permissions.associateWith(this::isPermissionGranted)))
    }

    @Subscribe
    fun onAskForPermissionRequestEvent(event: AskForPermissionRequestEvent) {
        val notGrantedPermissions = event.permissions.filterNot(this::isPermissionGranted).toTypedArray()
        val result = event.permissions.associateWith { true }

        if (notGrantedPermissions.isEmpty()) {
            event.respond(AskForPermissionResponseEvent(result))
            return
        }

        activity.launch(ActivityResultContracts.RequestMultiplePermissions(), notGrantedPermissions) {
            event.respond(AskForPermissionResponseEvent(result + it))
        }
    }
}
