package org.racehorse.utils

import org.greenrobot.eventbus.EventBus

/**
 * The event that must be handled on the posting thread.
 */
open class SyncHandlerEvent {

    var isHandled = false
        private set

    fun shouldHandle() = if (isHandled) false else {
        isHandled = true
        true
    }
}

inline fun <reified T> EventBus.postForSubscriber(eventFactory: () -> T): T? =
    if (hasSubscriberForEvent(T::class.java)) eventFactory().apply(::post) else null

inline fun <reified T : SyncHandlerEvent> EventBus.postForSyncHandler(eventFactory: () -> T) =
    if (hasSubscriberForEvent(T::class.java)) eventFactory().apply(::post).isHandled else false

fun EventBus.registerOnce(subscriber: Any) {
    if (!isRegistered(subscriber)) {
        register(subscriber)
    }
}
