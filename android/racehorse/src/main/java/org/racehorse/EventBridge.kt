package org.racehorse

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.NoSubscriberEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.SubscriberExceptionEvent
import org.racehorse.utils.KotlinTypeAdapterFactory
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
     *
     * Don't set this manually unless you really have to. This field is always populated during [respond] method call.
     */
    @Transient
    var eventBus: EventBus? = null

    /**
     * The request ID that links request that originated from the web view with the response.
     *
     * Don't set this manually unless you really have to. This field is always populated during [respond] method call.
     */
    @Transient
    var requestId = EventBridge.ORPHAN_REQUEST_ID

    /**
     * Executes a block and posts the returned event to the chain using the same event bus to which this event was
     * originally posted.
     *
     * If an exception is thrown in the block, then an [ExceptionEvent] is used as a response.
     */
    inline fun respond(block: () -> ChainableEvent) = respond(
        try {
            block()
        } catch (e: Throwable) {
            e.printStackTrace()
            ExceptionEvent(e)
        }
    )

    /**
     * Posts an [event] to the chain in the same event bus to which this event was originally posted.
     */
    fun respond(event: ChainableEvent) {
        event.eventBus = eventBus
        event.requestId = requestId

        eventBus?.post(event)
    }
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
    /**
     * The class name of the [Throwable] that caused the event.
     */
    val javaName = cause::class.java.name

    /**
     * The name of the [Throwable] that is used as an error name on the JavaScript side.
     */
    val name = cause::class.java.simpleName

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
 * Checks that there's a class that implements [WebEvent] or [NoticeEvent], and there's a registered subscriber that
 * handled the event of this class.
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
 * @param connectionKey The key of the `window` that exposes the connection Javascript interface.
 */
open class EventBridge(
    val webView: WebView,
    val eventBus: EventBus = EventBus.getDefault(),
    val gson: Gson = GsonBuilder()
        .serializeNulls()
        .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
        .registerTypeAdapter(Serializable::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Iterable::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Bundle::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Pair::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Date::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Uri::class.java, NaturalJsonAdapter())
        .registerTypeAdapter(Any::class.java, NaturalJsonAdapter())
        .create(),
    val connectionKey: String = "racehorseConnection",
) {

    companion object {
        const val TYPE_KEY = "type"
        const val PAYLOAD_KEY = "payload"
        const val ORPHAN_REQUEST_ID = -1
        const val NOTICE_REQUEST_ID = -2

        private const val TAG = "EventBridge"
    }

    /**
     * The cache of loaded event classes.
     */
    private val eventClassCache = HashMap<String, Class<*>>()

    /**
     * The ID that would be assigned to the next request.
     */
    private var nextRequestId = 0

    /**
     * The ID of the currently pending synchronous request, or [ORPHAN_REQUEST_ID] if there's no pending sync request.
     */
    private var syncRequestId = ORPHAN_REQUEST_ID

    /**
     * The response event for the currently pending synchronous request.
     */
    private var syncResponseEvent: ResponseEvent? = null

    open fun enable() = webView.addJavascriptInterface(this, connectionKey)

    open fun disable() = webView.removeJavascriptInterface(connectionKey)

    @JavascriptInterface
    open fun post(eventJson: String): String {
        val event = try {
            parseEvent(eventJson)
        } catch (e: Throwable) {
            return stringifyEvent(ExceptionEvent(IllegalArgumentException("Illegal event: $eventJson", e)))
        }

        if (event !is ChainableEvent) {
            eventBus.post(event)
            return stringifyEvent(VoidEvent())
        }

        return synchronized(this) {
            event.eventBus = eventBus
            event.requestId = nextRequestId++

            syncRequestId = event.requestId
            syncResponseEvent = null

            try {
                eventBus.post(event)
                syncResponseEvent?.let(::stringifyEvent) ?: event.requestId.toString()
            } catch (e: Throwable) {
                stringifyEvent(ExceptionEvent(e))
            } finally {
                syncRequestId = ORPHAN_REQUEST_ID
                syncResponseEvent = null
            }
        }
    }

    @Subscribe
    open fun onResponse(event: ResponseEvent) {
        if (event.requestId == ORPHAN_REQUEST_ID) {
            // The response event isn't related to any request event
            Log.i(TAG, "Received an orphan response event ${event::class.java.name}")
            return
        }
        if (syncRequestId == event.requestId) {
            // Synchronously return the response event
            syncResponseEvent = event
        } else {
            publish(event.requestId, event)
        }
    }

    @Subscribe
    open fun onNotice(event: NoticeEvent) {
        publish(NOTICE_REQUEST_ID, event)
    }

    @Subscribe
    open fun onNoSubscriber(event: NoSubscriberEvent) {
        val e = IllegalStateException("No subscribers for ${event.originalEvent::class.java.name}")
        e.printStackTrace()

        (event.originalEvent as? ChainableEvent)?.respond(ExceptionEvent(e))
    }

    @Subscribe
    open fun onSubscriberException(event: SubscriberExceptionEvent) {
        (event.causingEvent as? ChainableEvent)?.respond(ExceptionEvent(event.throwable))
    }

    @Subscribe
    open fun onIsSupported(event: IsSupportedEvent) {
        event.respond(
            IsSupportedEvent.ResultEvent(
                try {
                    val eventClass = Class.forName(event.eventType)

                    eventBus.hasSubscriberForEvent(eventClass) &&
                        (WebEvent::class.java.isAssignableFrom(eventClass) ||
                            NoticeEvent::class.java.isAssignableFrom(eventClass))
                } catch (_: Throwable) {
                    false
                }
            )
        )
    }

    protected fun parseEvent(eventJson: String): Any {
        val jsonObject = gson.fromJson(eventJson, JsonObject::class.java)

        val eventType = jsonObject.getAsJsonPrimitive(TYPE_KEY).asString

        val eventClass = eventClassCache.getOrPut(eventType) {
            try {
                Class.forName(eventType).also {
                    require(WebEvent::class.java.isAssignableFrom(it)) { "Unrecognized event type" }
                }
            } catch (_: ClassNotFoundException) {
                throw IllegalArgumentException("Unrecognized event type")
            }
        }

        return gson.fromJson(jsonObject.get(PAYLOAD_KEY) ?: JsonObject(), eventClass)
    }

    protected fun stringifyEvent(event: Any) =
        "{\"$TYPE_KEY\":\"${event::class.java.name}\",\"$PAYLOAD_KEY\":${gson.toJson(event)}}"

    protected fun publish(requestId: Int, event: Any) = webView.post {
        webView.evaluateJavascript(
            "(function(conn){" +
                "conn && conn.inbox && conn.inbox.publish([$requestId, ${stringifyEvent(event)}])" +
                "})(window.$connectionKey)",
            null
        )
    }
}
