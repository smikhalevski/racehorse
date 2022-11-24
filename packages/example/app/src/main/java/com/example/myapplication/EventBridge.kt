package com.example.myapplication

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.SubscriberExceptionEvent
import org.greenrobot.eventbus.ThreadMode

/**
 * The event that can be dispatched by the [EventBridge].
 */
open class Event {
    @Transient
    var requestId = -1L
}

infix fun <T : Event> T.causedBy(event: Event): T {
    this.requestId = event.requestId
    return this
}

/**
 * Events that extend this interface are pushed to the client and would resolve pending requests with matching
 * [Event.requestId].
 */
open class OutboxEvent : Event()

private const val JAVASCRIPT_OUTBOX_TAG = "JavascriptOutbox"

/**
 * The bridge enables seamless event transport between the [WebView] and the [EventBus].
 *
 * The client should push messages to the outbox:
 *
 * ```js
 * window.__outbox.push(JSON.stringify({
 *     requestId: Math.random() * 1e8
 *     javaClass: 'com.example.MyEvent',
 *     eventJson: JSON.stringify({
 *         value: 'Hello'
 *     })
 * }));
 * ```
 *
 * And the bridge would push messages to the inbox that the client should declare:
 *
 * ```js
 * window.__inbox = {
 *     push(response) {
 *         if (response.ok) {
 *             handleEvent(response.event)
 *         } else {
 *             handleError(response.message)
 *         }
 *     }
 * };
 * ```
 *
 * Note: `window.__inbox` may be set to an array if not yet initialized.
 */
class EventBridge(
    private val webView: WebView,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val gson: Gson = Gson(),
    private val inboxKey: String = "__inbox",
    private val outboxKey: String = "__outbox",
) {

    private val javascriptOutbox = JavascriptOutbox()

    /**
     * Connects the [WebView] to the [EventBus].
     */
    fun register() {
        webView.addJavascriptInterface(javascriptOutbox, outboxKey)
        eventBus.register(this)
    }

    /**
     * Disconnects the [WebView] from the [EventBus].
     */
    fun unregister() {
        webView.removeJavascriptInterface(outboxKey)
        eventBus.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onOutboxEvent(event: OutboxEvent) = pushToJavascriptInbox(Ok(event))

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSubscriberExceptionEvent(event: SubscriberExceptionEvent) {
        val causingEvent = event.causingEvent

        if (causingEvent is Event) {
            pushToJavascriptInbox(Err(causingEvent.requestId, event.throwable))
        }
    }

    private fun pushToJavascriptInbox(response: EventBridgeResponse) {
        val json = gson.toJson(response)

        webView.evaluateJavascript("(window.$inboxKey||(window.$inboxKey=[])).push($json)", null)
    }

    private inner class JavascriptOutbox {

        @JavascriptInterface
        fun push(message: String) {
            val event: Event
            val request = gson.fromJson(message, EventBridgeRequest::class.java)

            try {
                val eventClass = Class.forName(request.javaClass)

                Event::class.java.isAssignableFrom(eventClass) || throw IllegalArgumentException("Expected an event class: ${request.javaClass}")

                event = gson.fromJson(request.eventJson, eventClass) as Event
                event.requestId = request.requestId
            } catch (ex: Throwable) {
                Log.e(JAVASCRIPT_OUTBOX_TAG, "Cannot process an event", ex)

                pushToJavascriptInbox(Err(request.requestId, ex))
                return
            }

            eventBus.post(event)
        }
    }
}

/**
 * The request sent from the [WebView].
 */
private class EventBridgeRequest {
    /**
     * The unique request ID.
     */
    var requestId = -1L

    /**
     * The qualified name of the class that extends [Event].
     */
    lateinit var javaClass: String

    /**
     * The serialized instance of [javaClass].
     */
    lateinit var eventJson: String
}

/**
 * The response sent from the [EventBus].
 */
private open class EventBridgeResponse(val ok: Boolean, val requestId: Long)

/**
 * Success response from the [EventBus].
 */
private class Ok(val event: Event) : EventBridgeResponse(true, event.requestId) {
    val javaClass = event::class.qualifiedName
}

/**
 * Error response from the [EventBus].
 */
private class Err(requestId: Long, ex: Throwable) : EventBridgeResponse(true, requestId) {
    val javaClass = ex::class.qualifiedName
    val message = ex.message
    val stack = ex.stackTraceToString()
}
