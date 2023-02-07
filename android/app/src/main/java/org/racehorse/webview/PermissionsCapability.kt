package org.racehorse.webview

/**
 * Plugins with this capability are called when a particular permission must be granted.
 */
interface PermissionsCapability {

    fun askForPermission(permission: String, callback: (granted: Boolean) -> Unit): Boolean {
        return askForPermissions(arrayOf(permission)) {
            callback(it.getValue(permission))
        }
    }

    fun askForPermissions(permissions: Array<String>, callback: (statuses: Map<String, Boolean>) -> Unit): Boolean {
        return false
    }
}
