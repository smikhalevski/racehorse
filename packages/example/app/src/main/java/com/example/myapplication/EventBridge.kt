package com.example.myapplication

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.SubscriberExceptionEvent
import org.greenrobot.eventbus.ThreadMode

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

private const val TAG = "JavascriptOutbox"

/**
 * The bridge enables seamless event transport between the [WebView] and the [EventBus].
 *
 * The client should push messages to the outbox:
 * ```
 * window.__eventBridgeOutbox__.push(JSON.stringify({
 *     requestId: Math.random() * 1e8
 *     javaClass: 'com.example.MyEvent',
 *     eventJson: JSON.stringify({
 *         value: 'Hello'
 *     })
 * }))
 * ```
 *
 * And the bridge would push messages to the inbox that the client should declare:
 *
 * ```
 * window.__eventBridgeInbox__ = {
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
 * Note: `window.__eventBridgeInbox__` may be initialized with an array if bridge is injected before the client code is
 * executed.
 */
class EventBridge(
    private val webView: WebView,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val gson: Gson = Gson(),
    private val inboxKey: String = "__eventBridgeInbox__",
    private val outboxKey: String = "__eventBridgeOutbox__",
) {

    private val javascriptOutbox = JavascriptOutbox()

    fun register() {
        webView.addJavascriptInterface(javascriptOutbox, outboxKey)
        eventBus.register(this)
    }

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

    private fun pushToJavascriptInbox(response: Response) {
        val json = gson.toJson(response)

        webView.evaluateJavascript("(window.$inboxKey||(window.$inboxKey=[])).push($json)", null)
    }

    private inner class JavascriptOutbox {

        @JavascriptInterface
        fun push(message: String) {
            val event: Event
            val request = gson.fromJson(message, Request::class.java)

            try {
                val eventClass = Class.forName(request.javaClass)

                Event::class.java.isAssignableFrom(eventClass) || throw ClassNotFoundException(request.javaClass)

                event = gson.fromJson(request.eventJson, eventClass) as Event
                event.requestId = request.requestId
            } catch (ex: Throwable) {
                Log.e(TAG, "Event cannot be processed", ex)

                pushToJavascriptInbox(Err(request.requestId, ex))
                return
            }

            eventBus.post(event)
        }
    }
}

private class Request {
    var requestId = -1L

    lateinit var javaClass: String
    lateinit var eventJson: String
}

private open class Response(val ok: Boolean, val requestId: Long)

private class Ok(val event: Event) : Response(true, event.requestId) {
    val javaClass = event::class.qualifiedName
}

private class Err(requestId: Long, ex: Throwable) : Response(true, requestId) {
    val javaClass = ex::class.qualifiedName
    val message = ex.message
    val stack = ex.stackTraceToString()
}
