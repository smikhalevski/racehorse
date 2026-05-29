package org.racehorse

import android.net.http.SslError
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.ReceivedSslErrorEvent

/**
 * SSL termination and certificate error handling.
 *
 * @param shouldProceed A callback that receives an SSL error and must returns `true` if the error must be ignored.
 */
open class HttpsPlugin(val shouldProceed: (error: SslError) -> Boolean) {

    @Subscribe
    open fun onReceivedSslError(event: ReceivedSslErrorEvent) {
        if (!event.shouldHandle()) {
            return
        }
        if (shouldProceed(event.error)) {
            event.handler.proceed()
        } else {
            event.handler.cancel()
        }
    }
}
