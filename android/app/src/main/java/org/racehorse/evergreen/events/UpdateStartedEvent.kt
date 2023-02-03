package org.racehorse.evergreen.events

import org.racehorse.webview.events.Event

class UpdateStartedEvent(val mandatory: Boolean) : Event {
    override val requestId = -1L
}
