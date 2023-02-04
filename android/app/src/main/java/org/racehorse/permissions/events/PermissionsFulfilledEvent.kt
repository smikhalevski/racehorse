package org.racehorse.permissions.events

import org.racehorse.webview.events.OkEvent

class PermissionsFulfilledEvent(
    override val requestId: Long,
    val permissions: Array<out String>,
    val grantResults: Array<Int>
) : OkEvent
