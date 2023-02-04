package org.racehorse.permissions

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.permissions.events.PermissionsFulfilledEvent
import org.racehorse.permissions.events.PermissionsRequestedEvent
import org.racehorse.permissions.events.RequestPermissionsResultEvent
import java.util.concurrent.atomic.AtomicInteger

class RacehorsePermissionsManager(private val activity: Activity, private val eventBus: EventBus) {

    private val requestCodeIndex = AtomicInteger()

    private val requestCodeToRequestIdMap = HashMap<Int, Long>()

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun isUnknown(permission: String): Boolean {
        val permissionResult = ContextCompat.checkSelfPermission(activity, permission)

        return permissionResult != PackageManager.PERMISSION_GRANTED && permissionResult != PackageManager.PERMISSION_DENIED
    }

    private fun requestMissingPermissions(
        resultCode: Int,
        permissions: Array<out String>
    ): Boolean {
        val missingPermissions = permissions.filterNot(this::isGranted)
        if (missingPermissions.isEmpty()) {
            return false
        }
        ActivityCompat.requestPermissions(activity, missingPermissions.toTypedArray(), resultCode)
        return true
    }

    // From web to activity
    @Subscribe
    fun onPermissionsRequestedEvent(event: PermissionsRequestedEvent) {
        val requestCode = requestCodeIndex.incrementAndGet()

        requestCodeToRequestIdMap[requestCode] = event.requestId

        if (!requestMissingPermissions(requestCode, event.permissions)) {
            requestCodeToRequestIdMap.remove(requestCode)
            eventBus.post(PermissionsFulfilledEvent(event.requestId, emptyArray(), emptyArray()))
        }
    }

    // From activity to web
    @Subscribe
    fun onRequestPermissionsResultEvent(event: RequestPermissionsResultEvent) {
        val requestId = requestCodeToRequestIdMap[event.requestCode]

        if (requestId != null) {
            requestCodeToRequestIdMap.remove(event.requestCode)
            eventBus.post(PermissionsFulfilledEvent(requestId, event.permissions, event.grantResults.toTypedArray()))
        }
    }
}
