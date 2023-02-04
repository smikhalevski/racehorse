package org.racehorse.evergreen.events

import org.racehorse.webview.events.OkEvent

class UpdateFailedEvent(val mandatory: Boolean) : OkEvent {
    override val requestId = -1L
}
