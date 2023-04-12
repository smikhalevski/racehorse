package org.racehorse

import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.ReceivedSslErrorEvent

/**
 * SSL termination and certificate error handling.
 */
open class HttpsPlugin {

    @Subscribe
    open fun onReceivedSslError(event: ReceivedSslErrorEvent) {
        if (event.shouldHandle()) {
            event.handler.proceed()
        }
    }
}
