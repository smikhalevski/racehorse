package org.racehorse

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.EventBusCapability
import org.racehorse.webview.PermissionsCapability
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
 * Check permission statuses and ask for permissions.
 */
class PermissionsPlugin(private val activity: ComponentActivity) : Plugin(), EventBusCapability, PermissionsCapability {

    override fun onAskForPermissions(
        permissions: Array<String>,
        callback: (statuses: Map<String, Boolean>) -> Unit
    ): Boolean {
        val statuses = permissions.associateWith { true }
        val notGrantedPermissions = statuses.keys.filterNot(activity::isPermissionGranted).toTypedArray()

        return if (notGrantedPermissions.isEmpty()) {
            callback(statuses)
            true
        } else {
            activity.launchForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
                notGrantedPermissions
            ) {
                callback(statuses + it)
            }
        }
    }

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
            event.permissions.distinct().associateWith(activity::isPermissionGranted)
        ))
    }

    @Subscribe
    fun onAskForPermissionRequestEvent(event: AskForPermissionRequestEvent) {
        onAskForPermissions(event.permissions) {
            postToChain(event, AskForPermissionResponseEvent(it))
        }
    }
}
