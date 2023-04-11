package org.racehorse

import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.ReceivedSslErrorEvent

open class HttpsController {

    @Subscribe
    open fun onReceivedSslError(event: ReceivedSslErrorEvent) {
        if (event.shouldHandle()) {
            event.handler.proceed()
        }
    }
}
