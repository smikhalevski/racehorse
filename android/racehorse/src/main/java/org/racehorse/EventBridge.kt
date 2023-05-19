package org.racehorse

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.NoSubscriberEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.SubscriberExceptionEvent
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.NaturalAdapter
import java.io.Serializable
import java.util.concurrent.FutureTask

/**
 * An event posted from the web view. Only events that implement this interface are "visible" to the web application.
 */
interface WebEvent : Serializable

/**
 * An event published by Android for subscribers in web view.
 */
interface NoticeEvent : Serializable

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
    val stackTrace = cause.stackTraceToString()
}

/**
 * The event bridge enables communication between the app in the web view and Android-native code.
 *
 * @param webView The [WebView] to which the event bridge will add the connection Javascript interface.
 * @param eventBus The event bus to which events are posted.
 * @param connectionKey The key of the `window` that exposes the connection Javascript interface.
 * @param gson The [Gson] instance that is used for event serialization.
 */
open class EventBridge(
    private val webView: WebView,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val gson: Gson = NaturalAdapter().let {
        GsonBuilder()
            .serializeNulls()
            .registerTypeAdapter(Serializable::class.java, it)
            .registerTypeAdapter(Bundle::class.java, it)
            .registerTypeAdapter(Any::class.java, it)
            .create()
    },
    private val connectionKey: String = "racehorseConnection"
) {

    init {
        webView.addJavascriptInterface(this, connectionKey)
    }

    /**
     * The cache of loaded event classes.
     */
    private val eventClasses = HashMap<String, Class<*>>()

    /**
     * The handler to which events are posted and on which synchronous responses are expected.
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * The ID of the current request.
     */
    private var requestId = 0

    /**
     * The ID of the currently pending synchronous request.
     */
    private var syncRequestId = -1

    /**
     * The synchronous response.
     */
    private var syncResponseEvent: ResponseEvent? = null

    /**
     * Returns the class associated with the event type.
     */
    private fun getEventClass(eventType: String) = eventClasses.getOrPut(eventType) {
        Class.forName(eventType).also {
            require(WebEvent::class.java.isAssignableFrom(it)) { "Expected an event: $eventType" }
        }
    }

    private fun serializeEvent(event: Any) = gson.toJson(JsonObject().apply {
        addProperty("type", event::class.java.name)
        add("payload", gson.toJsonTree(event))
    })

    private fun sendAsyncEvent(requestId: Int, event: Any) {
        webView.evaluateJavascript(
            "(function(connection){" +
                "connection && connection.inbox && connection.inbox.publish([$requestId, ${serializeEvent(event)}])" +
                "})(window.$connectionKey)",
            null
        )
    }

    @JavascriptInterface
    fun post(eventJson: String): String {
        val event = try {
            gson.fromJson(eventJson, JsonObject::class.java).run {
                gson.fromJson(get("payload") ?: JsonObject(), getEventClass(get("type").asString))
            }
        } catch (ex: Throwable) {
            return serializeEvent(ExceptionEvent(ex))
        }
        if (event !is ChainableEvent) {
            eventBus.post(event)
            return serializeEvent(VoidEvent())
        }

        val requestId = requestId++
        syncRequestId = requestId

        return try {
            FutureTask { eventBus.post(event.setRequestId(requestId)) }.apply(mainHandler::post).get()
            syncResponseEvent?.let(::serializeEvent) ?: requestId.toString()
        } catch (ex: Throwable) {
            serializeEvent(ExceptionEvent(ex))
        } finally {
            syncRequestId = -1
            syncResponseEvent = null
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onResponse(event: ResponseEvent) {
        require(event.requestId >= 0) { "Expected a request ID to be set for a response event" }

        if (syncRequestId == event.requestId) {
            syncResponseEvent = event
        } else {
            sendAsyncEvent(event.requestId, event)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotice(event: NoticeEvent) {
        sendAsyncEvent(-1, event)
    }

    @Subscribe
    fun onNoSubscriber(event: NoSubscriberEvent) {
        (event.originalEvent as? RequestEvent)?.let {
            eventBus.post(ExceptionEvent(IllegalStateException("No subscribers for $it")).setRequestId(it.requestId))
        }
    }

    @Subscribe
    fun onSubscriberException(event: SubscriberExceptionEvent) {
        when (val causingEvent = event.causingEvent) {

            is ExceptionEvent -> causingEvent.cause.printStackTrace()

            is RequestEvent -> eventBus.post(ExceptionEvent(event.throwable).setRequestId(causingEvent.requestId))
        }
    }
}
