package org.racehorse

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.EventBusCapability
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent

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
class PermissionsPlugin(private val activity: ComponentActivity) : Plugin(), EventBusCapability {

    @Subscribe
    fun onShouldShowRequestPermissionRationaleRequestEvent(event: ShouldShowRequestPermissionRationaleRequestEvent) {
        postToChain(event, ShouldShowTaskPermissionRationaleResponseEvent(
            event.permissions.distinct().associateWith {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        ))
    }

    @Subscribe
    fun onIsPermissionGrantedRequestEvent(event: IsPermissionGrantedRequestEvent) {
        postToChain(event, IsPermissionGrantedResponseEvent(
            event.permissions.distinct().associateWith {
                isPermissionGranted(activity, it)
            }
        ))
    }

    @Subscribe
    fun onAskForPermissionRequestEvent(event: AskForPermissionRequestEvent) {
        val result = event.permissions.associateWith { true }
        val notGrantedPermissions = result.keys.filterNot { isPermissionGranted(activity, it) }.toTypedArray()

        if (notGrantedPermissions.isEmpty()) {
            postToChain(event, AskForPermissionResponseEvent(result))
            return
        }

        activity.launchForActivityResult(ActivityResultContracts.RequestMultiplePermissions(), notGrantedPermissions) {
            postToChain(event, AskForPermissionResponseEvent(result + it))
        }
    }
}
