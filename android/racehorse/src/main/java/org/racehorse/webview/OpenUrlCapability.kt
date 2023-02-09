package org.racehorse.webview

import android.net.Uri

/**
 * Plugins with this capability allow user to open URLs in external applications.
 */
interface OpenUrlCapability {

    fun onOpenUrl(uri: Uri) = false
}
