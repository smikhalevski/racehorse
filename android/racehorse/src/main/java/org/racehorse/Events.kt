package org.racehorse

/**
 * An event posted from the web view. Only events that implement this interface are allowed to pass trough the event
 * bridge.
 */
interface WebEvent

/**
 * An event published by Android for subscribers in web view.
 */
interface NoticeEvent

/**
 * An event that is the part of request-response chain of events.
 */
open class ChainableEvent {

    /**
     * The ID of the original request that started the chain of events.
     */
    @Transient
    var requestId: Int = -1
        private set

    fun setRequestId(requestId: Int): ChainableEvent {
        require(requestId >= 0) { "Unexpected request ID" }

        this.requestId = requestId
        return this
    }
}

/**
 * An event in an event chain that originated from the web request. The chain expects a [ResponseEvent] to be posted to
 * fulfill the pending promise on the web side.
 */
open class RequestEvent : ChainableEvent(), WebEvent

/**
 * An event that is published to the web, denoting an end of a request.
 *
 * @param ok If `true` then the request promise is fulfilled, otherwise it is rejected.
 */
open class ResponseEvent(val ok: Boolean = true) : ChainableEvent()

/**
 * Response with no payload.
 */
class VoidResponseEvent : ResponseEvent()

/**
 * Response that describes an occurred exception.
 */
class ExceptionResponseEvent(@Transient val cause: Throwable) : ResponseEvent(false) {
    val stackTrace = cause.stackTraceToString()
}
