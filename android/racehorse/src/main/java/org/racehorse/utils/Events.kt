package org.racehorse.utils

import org.greenrobot.eventbus.EventBus
import org.racehorse.ChainableEvent

open class HandlerEvent {
    var isHandled = false
        private set

    fun shouldHandle(): Boolean {
        if (isHandled) {
            return false
        }
        isHandled = true
        return true
    }
}

fun EventBus.postToChain(previousEvent: ChainableEvent, nextEvent: ChainableEvent) {
    post(nextEvent.setRequestId(previousEvent.requestId))
}

inline fun <reified T> EventBus.postForSubscriber(eventFactory: () -> T) {
    if (hasSubscriberForEvent(T::class.java)) {
        post(eventFactory())
    }
}

inline fun <reified T : HandlerEvent> EventBus.postForHandler(eventFactory: () -> T) =
    if (hasSubscriberForEvent(T::class.java)) {
        val event = eventFactory()
        post(event)
        event.isHandled
    } else false