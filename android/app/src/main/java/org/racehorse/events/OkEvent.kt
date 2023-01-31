package org.racehorse.events

/**
 * The event that is pushed to the web app to signal that something was successfully completed.
 *
 * @param requestId The ID of the request that is fulfilled, or pass -1 to notify subscribers on the web side.
 */
open class OkEvent(requestId: Long) : Event(requestId)
