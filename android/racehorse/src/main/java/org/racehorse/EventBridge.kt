package org.racehorse

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.NoSubscriberEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.SubscriberExceptionEvent
import org.racehorse.utils.NaturalJsonAdapter
import java.io.Serializable
import java.util.Date

/**
 * An event posted from the WebView.
 *
 * Only events that implement this interface are "visible" to the web application.
 */
interface WebEvent : Serializable

/**
 * An event published by Android for subscribers in the WebView.
 */
interface NoticeEvent : Serializable

/**
 * An event that is the part of request-response chain of events.
 */
open class ChainableEvent {

    /**
     * The event bus to which responses to this event are posted, or `null` if origin wasn't set for this event.
     */
    @Transient
    var eventBus: EventBus? = null
        private set

    /**
     * The request ID that links request that originated from the web view with the response.
     */
    @Transient
    var requestId: Int = -1
        private set

    /**
     * Sets the origin for this event to which consequent events in chain are posted.
     */
    fun setOrigin(eventBus: EventBus, requestId: Int): ChainableEvent {
        this.eventBus = eventBus
        this.requestId = requestId
        return this
    }

    /**
     * Executes a block and posts the returned event to the chain in the same event bus to which this event was
     * originally posted. If an exception is thrown in the block, then an [ExceptionEvent] is used as a response.
     */
    fun respond(block: () -> ChainableEvent) {
        val eventBus = checkNotNull(eventBus) { "Event has no origin" }

        eventBus.post(ExceptionEvent.unless(block).setOrigin(eventBus, requestId))
    }

    /**
     * Posts an [event] to the chain in the same event bus to which this event was originally posted.
     */
    fun respond(event: ChainableEvent) = respond { event }
}

/**
 * An event in an event chain that originated from the web request. The chain expects a [ResponseEvent] to be posted to
 * fulfill the pending promise on the web side.
 */
abstract class RequestEvent : ChainableEvent(), WebEvent

/**
 * An event that is published to the web, denoting an end of a request.
 */
abstract class ResponseEvent : ChainableEvent(), Serializable

/**
 * Response with no payload. Use this event to commit the chain of events that doesn't imply a response. Chain of events
 * guarantees that if an exception is thrown then pending promise is rejected.
 */
class VoidEvent : ResponseEvent()

/**
 * Response that describes an occurred exception.
 */
class ExceptionEvent(@Transient val cause: Throwable) : ResponseEvent() {

    companion object {
        /**
         * Returns an event from the block, or an [ExceptionEvent] if an error is thrown.
         */
        fun unless(block: () -> ChainableEvent): ChainableEvent = try {
            block()
        } catch (e: Throwable) {
            e.printStackTrace()
            ExceptionEvent(e)
        }
    }

    /**
     * The class name of the [Throwable] that caused the event.
     */
    val name = cause::class.java.name

    /**
     * The detail message string.
     */
    val message = cause.message.orEmpty()

    /**
     * The serialized stack trace.
     */
    val stack = cause.stackTraceToString()
}

/**
 * Checks that there's a class that implements [WebEvent] or [NoticeEvent].
 */
class IsSupportedEvent(val eventType: String) : RequestEvent() {
    class ResultEvent(val isSupported: Boolean) : ResponseEvent()
}

/**
 * The event bridge enables communication between the app in the WebView and Android-native code.
 *
 * @param webView The [WebView] to which the event bridge will add the connection Javascript interface.
 * @param eventBus The event bus to which events are posted.
 * @param gson The [Gson] instance that is used for event serialization.
 * @param handler The handler that is used to communicate with the [webView].
 * @param connectionKey The key of the `window` that exposes the connection Javascript interface.
 */
open class EventBridge(
    val webView: WebView,
    val eventBus: EventBus = EventBus.getDefault(),
    val gson: Gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapter(Serializable::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Bundle::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Date::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Any::class.java, NaturalJsonAdapter())
        .create(),
    val handler: Handler = Handler(Looper.getMainLooper()),
    val connectionKey: String = "racehorseConnection",
) {

    companion object {
        const val TYPE_KEY = "type"
        const val PAYLOAD_KEY = "payload"
    }

    init {
        webView.addJavascriptInterface(this, connectionKey)
    }

    /**
     * The cache of loaded event classes.
     */
    private val eventClasses = HashMap<String, Class<*>>()

    /**
     * The ID that would be assigned to the next request.
     */
    private var nextRequestId = 0

    /**
     * The ID of the currently pending synchronous request, or -1 if there's no pending synchronous request.
     */
    private var syncRequestId = -1

    /**
     * The response event for the currently pending synchronous request.
     */
    private var syncResponseEvent: ResponseEvent? = null

    @JavascriptInterface
    open fun post(requestJson: String): String {
        val event = try {
            gson.fromJson(requestJson, JsonObject::class.java).run {
                gson.fromJson(get(PAYLOAD_KEY) ?: JsonObject(), getEventClass(get(TYPE_KEY).asString))
            }
        } catch (e: Throwable) {
            return getEventJson(ExceptionEvent(e))
        }

        if (event !is ChainableEvent) {
            eventBus.post(event)
            return getEventJson(VoidEvent())
        }

        return synchronized(this) {
            event.setOrigin(eventBus, nextRequestId++)

            syncRequestId = event.requestId
            syncResponseEvent = null

            try {
                eventBus.post(event)
                syncResponseEvent?.let(::getEventJson) ?: event.requestId.toString()
            } catch (e: Throwable) {
                getEventJson(ExceptionEvent(e))
            } finally {
                syncRequestId = -1
                syncResponseEvent = null
            }
        }
    }

    @Subscribe
    open fun onResponse(event: ResponseEvent) {
        require(event.requestId != -1) { "The response event isn't related to any request event" }

        if (syncRequestId == event.requestId) {
            // Synchronously return the response event
            syncResponseEvent = event
        } else {
            publishEvent(event.requestId, event)
        }
    }

    @Subscribe
    open fun onNotice(event: NoticeEvent) {
        publishEvent(-2, event)
    }

    @Subscribe
    open fun onNoSubscriber(event: NoSubscriberEvent) {
        val e = IllegalStateException("No subscribers for ${event.originalEvent::class.java.name}")
        e.printStackTrace()

        (event.originalEvent as? ChainableEvent)?.respond(ExceptionEvent(e))
    }

    @Subscribe
    open fun onSubscriberException(event: SubscriberExceptionEvent) {
        when (val causingEvent = event.causingEvent) {

            is ExceptionEvent -> causingEvent.cause.printStackTrace()

            is RequestEvent -> causingEvent.respond(ExceptionEvent(event.throwable))
        }
    }

    @Subscribe
    open fun onIsSupported(event: IsSupportedEvent) {
        event.respond(
            IsSupportedEvent.ResultEvent(
                try {
                    val type = Class.forName(event.eventType)
                    WebEvent::class.java.isAssignableFrom(type) || NoticeEvent::class.java.isAssignableFrom(type)
                } catch (_: Throwable) {
                    false
                }
            )
        )
    }

    /**
     * Returns the class associated with the event type.
     */
    private fun getEventClass(eventType: String) = eventClasses.getOrPut(eventType) {
        Class.forName(eventType).also {
            require(WebEvent::class.java.isAssignableFrom(it)) { "Not an event: $eventType" }
        }
    }

    private fun getEventJson(event: Any) = gson.toJson(JsonObject().apply {
        add(TYPE_KEY, JsonPrimitive(event::class.java.name))
        add(PAYLOAD_KEY, gson.toJsonTree(event))
    })

    private fun publishEvent(requestId: Int, event: Any) = handler.post {
        webView.evaluateJavascript(
            "(function(conn){" +
                "conn && conn.inbox && conn.inbox.publish([$requestId, ${getEventJson(event)}])" +
                "})(window.$connectionKey)",
            null
        )
    }
}
