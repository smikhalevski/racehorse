package org.racehorse.events

/**
 * The event that is pushed to the web app to signal that an error has occurred.
 */
open class ErrEvent(requestId: Long, cause: Throwable) : Event(requestId) {
    val name: String = cause.javaClass.name
    val message = cause.message
    val stack = cause.stackTraceToString()
}
