package org.racehorse

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.serializer
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.NoSubscriberEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.SubscriberExceptionEvent
import org.racehorse.serializers.FileSerializer
import org.racehorse.serializers.IntentSerializer
import org.racehorse.serializers.ThrowableSerializer
import org.racehorse.serializers.UriSerializer
import org.racehorse.utils.loadClass
import kotlin.reflect.full.isSubclassOf

/**
 * An event posted from the WebView.
 *
 * Only events that implement this interface are "visible" to the web application.
 */
interface WebEvent

/**
 * An event published by Android for subscribers in the WebView.
 */
interface NoticeEvent

/**
 * An event that is the part of request-response chain of events.
 */
abstract class ChainableEvent {

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
abstract class ResponseEvent : ChainableEvent()

/**
 * Response with no payload. Use this event to commit the chain of events that doesn't imply a response. Chain of events
 * guarantees that if an exception is thrown then pending promise is rejected.
 */
@Serializable
object VoidEvent : ResponseEvent()

/**
 * Response that describes an occurred exception.
 */
@Serializable
class ExceptionEvent(@Transient val cause: Throwable? = null) : ResponseEvent() {
    /**
     * The class name of the [Throwable] that caused the event.
     */
    val javaName = cause!!::class.java.name

    /**
     * The name of the [Throwable] that is used as an error name on the JavaScript side.
     */
    val name = cause!!::class.java.simpleName

    /**
     * The detail message string.
     */
    val message = cause!!.message.orEmpty()

    /**
     * The serialized stack trace.
     */
    val stack = cause!!.stackTraceToString()
}

/**
 * Checks that there's a class that implements [WebEvent] or [NoticeEvent], and there's a registered subscriber that
 * handled the event of this class.
 */
@Serializable
class IsSupportedEvent(val eventType: String) : RequestEvent() {

    @Serializable
    class ResultEvent(val isSupported: Boolean) : ResponseEvent()
}

/**
 * The event bridge enables communication between the app in the WebView and Android-native code.
 *
 * @param webView The [WebView] to which the event bridge will add the connection Javascript interface.
 * @param eventBus The event bus to which events are posted.
 * @param json The serializer instance.
 * @param connectionKey The key of the `window` that exposes the connection Javascript interface.
 */
open class EventBridge(
    val webView: WebView,
    val eventBus: EventBus = EventBus.getDefault(),
    val json: Json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            contextual(FileSerializer)
            contextual(IntentSerializer)
            contextual(ThrowableSerializer)
            contextual(UriSerializer)
        }
    },
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
    @InternalSerializationApi
    open fun post(eventJson: String): String {
        val event = try {
            parseEvent(eventJson)
        } catch (e: Throwable) {
            return encodeEventToJson(ExceptionEvent(IllegalArgumentException("Illegal event: $eventJson", e)))
        }

        if (event !is ChainableEvent) {
            eventBus.post(event)
            return encodeEventToJson(VoidEvent)
        }

        return synchronized(this) {
            event.eventBus = eventBus
            event.requestId = nextRequestId++

            syncRequestId = event.requestId
            syncResponseEvent = null

            try {
                eventBus.post(event)
                syncResponseEvent?.let(::encodeEventToJson) ?: event.requestId.toString()
            } catch (e: Throwable) {
                encodeEventToJson(ExceptionEvent(e))
            } finally {
                syncRequestId = ORPHAN_REQUEST_ID
                syncResponseEvent = null
            }
        }
    }

    @Subscribe
    @InternalSerializationApi
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
    @InternalSerializationApi
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
                    val eventClass = loadClass(event.eventType)

                    // There are active subscribers
                    eventBus.hasSubscriberForEvent(eventClass.java) &&
                        // Class is an event
                        (eventClass.isSubclassOf(WebEvent::class) || eventClass.isSubclassOf(NoticeEvent::class))
                } catch (_: Throwable) {
                    false
                }
            )
        )
    }

    @InternalSerializationApi
    protected fun parseEvent(eventJson: String): Any {
        val eventObject = json.parseToJsonElement(eventJson).jsonObject

        val eventType = eventObject[TYPE_KEY]?.jsonPrimitive?.content
            ?: throw IllegalArgumentException("Expected the event type")

        val eventClass = try {
            loadClass(eventType).apply {
                require(isSubclassOf(WebEvent::class)) { "Unrecognized event type" }
            }
        } catch (_: ClassNotFoundException) {
            throw IllegalArgumentException("Unrecognized event type")
        }

        val eventPayload = eventObject.jsonObject[PAYLOAD_KEY] ?: JsonObject(HashMap())

        return json.decodeFromJsonElement(eventClass.serializer(), eventPayload)
    }

    @InternalSerializationApi
    @Suppress("UNCHECKED_CAST")
    protected fun encodeEventToJson(event: Any): String =
        "{" +
            "\"$TYPE_KEY\":\"${event::class.java.name}\"," +
            "\"$PAYLOAD_KEY\":${
                json.encodeToString(event::class.serializer() as KSerializer<Any>, event)
            }}"

    @InternalSerializationApi
    protected fun publish(requestId: Int, event: Any): Boolean = webView.post {
        val js = "(function(conn){" +
            "conn && conn.inbox && conn.inbox.publish([$requestId, ${encodeEventToJson(event)}])" +
            "})(window.$connectionKey)"

        webView.evaluateJavascript(js, null)
    }
}
