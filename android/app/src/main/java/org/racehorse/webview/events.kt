package org.racehorse.webview

import org.greenrobot.eventbus.EventBus

/**
 * Any event posted from web.
 */
interface InboxEvent

/**
 * Event pushed to web that is provided to web subscribers.
 */
interface AlertEvent

/**
 * An event that is the part of request-response chain of events.
 */
interface ChainableEvent {
    /**
     * The original request ID.
     */
    var requestId: Int
}

/**
 * Posts the event to event bus and sets its [ChainableEvent.requestId] to the ID of this event.
 */
fun ChainableEvent.postToChain(event: ChainableEvent) {
    event.requestId = this.requestId
    EventBus.getDefault().post(event)
}

/**
 * The event that participates in request-response chain of events initiated from the web.
 */
open class BasicChainableEvent(override var requestId: Int) : ChainableEvent

/**
 * An event in an event chain that originated from the web request. The chain expects a [ResponseEvent] to be posted to
 * fulfill the pending promise on the web side.
 */
open class RequestEvent : BasicChainableEvent(-1), InboxEvent

/**
 * An event that is pushed to web denoting an end of a request.
 *
 * @param ok If `true` then the request promise is fulfilled, otherwise it is rejected.
 */
open class ResponseEvent(val ok: Boolean = true) : BasicChainableEvent(-1)

/**
 * Pushed to web in response to non-chainable event posted from web.
 */
class AutoCloseResponseEvent(override var requestId: Int) : ResponseEvent()

/**
 * An event that rejects the request promise.
 */
class ErrorResponseEvent(requestId: Int, @Transient val cause: Throwable) : ResponseEvent(false) {
    constructor(cause: Throwable) : this(-1, cause)
}
