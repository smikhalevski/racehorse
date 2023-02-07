package org.racehorse.webview

import org.greenrobot.eventbus.EventBus

/**
 * Plugins with this capability are automatically registered in [EventBus].
 */
interface EventBusCapability {

    var eventBus: EventBus

    fun post(event: Any) {
        eventBus.post(event)
    }

    fun postToChain(causingEvent: ChainableEvent, event: ChainableEvent) {
        post(event.setRequestId(causingEvent.requestId))
    }
}
