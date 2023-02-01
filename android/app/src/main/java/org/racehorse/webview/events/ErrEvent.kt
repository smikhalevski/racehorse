package org.racehorse.webview.events

/**
 * The event that is pushed to the web app to signal that an error has occurred.
 */
open class ErrEvent(override val requestId: Long, cause: Throwable) : Event {
    val name: String = cause.javaClass.name
    val message = cause.message
    val stack = cause.stackTraceToString()
}
