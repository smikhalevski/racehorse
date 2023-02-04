package org.racehorse.evergreen.events

import org.racehorse.webview.events.OkEvent

class UpdateStartedEvent(val mandatory: Boolean) : OkEvent {
    override val requestId = -1L
}
