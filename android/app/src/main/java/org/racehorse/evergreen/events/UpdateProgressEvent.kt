package org.racehorse.evergreen.events

import org.racehorse.webview.events.Event

class UpdateProgressEvent(val contentLength: Int, val readLength: Long) : Event {
    override val requestId = -1L
}
