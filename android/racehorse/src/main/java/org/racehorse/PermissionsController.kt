package org.racehorse

import android.Manifest
import android.webkit.PermissionRequest
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.isPermissionGranted
import org.racehorse.utils.launchForActivityResult
import org.racehorse.utils.postToChain
import org.racehorse.webview.GeolocationPermissionsShowPromptEvent
import org.racehorse.webview.PermissionRequestEvent

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
 * Requests permissions to be granted to the app.
 */
class AskForPermissionRequestEvent(val permissions: Array<String>) : RequestEvent()

class AskForPermissionResponseEvent(val statuses: Map<String, Boolean>) : ResponseEvent()

/**
 * Check permission statuses and ask for permissions.
 */
open class PermissionsController(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    @Subscribe
    open fun onGeolocationPermissionsShowPrompt(event: GeolocationPermissionsShowPromptEvent) {
        askForPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            event.callback.invoke(event.origin, it.containsValue(true), false)
        }
    }

    @Subscribe
    open fun onPermissionRequest(event: PermissionRequestEvent) {
        val permissions = HashSet<String>()

        if (event.request.resources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (event.request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissions.isEmpty() || !event.shouldHandle()) {
            return
        }

        askForPermissions(permissions.toTypedArray()) {
            val resources = HashSet<String>()

            if (it[Manifest.permission.CAMERA] == true) {
                resources.add(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            }
            if (it[Manifest.permission.RECORD_AUDIO] == true) {
                resources.add(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            }
            if (resources.isEmpty()) {
                event.request.deny()
            } else {
                event.request.grant(resources.toTypedArray())
            }
        }
    }

    @Subscribe
    open fun onShouldShowRequestPermissionRationale(event: ShouldShowRequestPermissionRationaleRequestEvent) {
        eventBus.postToChain(event, ShouldShowTaskPermissionRationaleResponseEvent(
            event.permissions.distinct().associateWith {
                ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
            }
        ))
    }

    @Subscribe
    open fun onIsPermissionGranted(event: IsPermissionGrantedRequestEvent) {
        eventBus.postToChain(
            event,
            IsPermissionGrantedResponseEvent(event.permissions.distinct().associateWith(activity::isPermissionGranted))
        )
    }

    @Subscribe
    open fun onAskForPermission(event: AskForPermissionRequestEvent) {
        askForPermissions(event.permissions) {
            eventBus.postToChain(event, AskForPermissionResponseEvent(it))
        }
    }

    protected fun askForPermissions(
        permissions: Array<String>,
        callback: (statuses: Map<String, Boolean>) -> Unit
    ): Boolean {
        val statuses = permissions.associateWith { true }
        val missingPermissions = permissions.filterNot(activity::isPermissionGranted).toTypedArray()

        if (missingPermissions.isEmpty()) {
            callback(statuses)
            return true
        }

        return activity.launchForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
            missingPermissions
        ) {
            callback(statuses + it)
        }
    }
}