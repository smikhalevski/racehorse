package org.racehorse.webview

import android.net.Uri

/**
 * Plugins with this capability allow user to open URLs in external apps.
 */
interface OpenUrlCapability {

    fun onOpenUrl(uri: Uri) = false
}
