package org.racehorse

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent
import org.racehorse.webview.chain

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
class PermissionsPlugin : Plugin() {
    private fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    @Subscribe
    fun onShouldShowRequestPermissionRationaleRequestEvent(event: ShouldShowRequestPermissionRationaleRequestEvent) {
        post(event.chain(ShouldShowTaskPermissionRationaleResponseEvent(
            event.permissions.associateWith {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        )))
    }

    @Subscribe
    fun onIsPermissionGrantedRequestEvent(event: IsPermissionGrantedRequestEvent) {
        post(event.chain(IsPermissionGrantedResponseEvent(event.permissions.associateWith(this::isPermissionGranted))))
    }

    @Subscribe
    fun onAskForPermissionRequestEvent(event: AskForPermissionRequestEvent) {
        val notGrantedPermissions = event.permissions.filterNot(this::isPermissionGranted).toTypedArray()
        val result = event.permissions.associateWith { true }

        if (notGrantedPermissions.isEmpty()) {
            post(event.chain(AskForPermissionResponseEvent(result)))
            return
        }

        activity.launchForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), notGrantedPermissions) {
            post(event.chain(AskForPermissionResponseEvent(result + it)))
        }
    }
}
