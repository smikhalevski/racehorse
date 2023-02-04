package org.racehorse.evergreen.events

import org.racehorse.webview.events.OkEvent

class UpdateProgressEvent(val contentLength: Int, val readLength: Long) : OkEvent {
    override val requestId = -1L
}
