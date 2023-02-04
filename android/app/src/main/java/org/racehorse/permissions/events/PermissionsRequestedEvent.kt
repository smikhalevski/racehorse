package org.racehorse.permissions.events

import org.racehorse.webview.events.WebEvent

class PermissionsRequestedEvent(override val requestId: Long, val permissions: Array<out String>) : WebEvent
