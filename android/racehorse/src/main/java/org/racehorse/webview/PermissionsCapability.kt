package org.racehorse.webview

/**
 * Plugins with this capability are called when a particular permission must be granted.
 */
interface PermissionsCapability {

    fun onAskForPermission(permission: String, callback: (granted: Boolean) -> Unit): Boolean {
        return onAskForPermissions(arrayOf(permission)) {
            callback(it.getValue(permission))
        }
    }

    fun onAskForPermissions(permissions: Array<String>, callback: (statuses: Map<String, Boolean>) -> Unit) = false
}
