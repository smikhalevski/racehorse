package org.racehorse

import android.os.Bundle
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
import java.util.concurrent.atomic.AtomicInteger

/**
 * An event posted from the web view. Only events that implement this interface are "visible" to the web application.
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
abstract class RequestEvent : ChainableEvent(), WebEvent

/**
 * An event that is published to the web, denoting an end of a request.
 */
abstract class ResponseEvent : ChainableEvent()

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
    private val gson: Gson = naturalGson,
    private val connectionKey: String = "racehorseConnection"
) {

    companion object {
        val naturalGson: Gson by lazy {
            val naturalAdapter = NaturalAdapter()

            GsonBuilder()
                .serializeNulls()
                .registerTypeAdapter(Serializable::class.java, naturalAdapter)
                .registerTypeAdapter(Bundle::class.java, naturalAdapter)
                .registerTypeAdapter(Any::class.java, naturalAdapter)
                .create()
        }
    }

    private var requestId = AtomicInteger()

    init {
        webView.addJavascriptInterface(this, connectionKey)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onResponse(event: ResponseEvent) {
        require(event.requestId >= 0) { "Expected a request ID to be set for a response event" }

        publish(event.requestId, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNotice(event: NoticeEvent) {
        publish(-1, event)
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

    @JavascriptInterface
    fun post(eventJson: String): Int {
        val requestId = requestId.getAndIncrement()

        val event = try {
            val jsonObject = gson.fromJson(eventJson, JsonObject::class.java)
            val eventClass = Class.forName(jsonObject["type"].asString)

            jsonObject.remove("requestId")
            jsonObject.remove("type")

            require(WebEvent::class.java.isAssignableFrom(eventClass)) { "Not an event: $eventClass" }

            gson.fromJson(jsonObject, eventClass)
        } catch (throwable: Throwable) {
            eventBus.post(ExceptionEvent(throwable).setRequestId(requestId))
            return requestId
        }

        if (event is ChainableEvent) {
            eventBus.post(event.setRequestId(requestId))
            return requestId
        }

        eventBus.post(event)
        eventBus.post(VoidEvent().setRequestId(requestId))
        return requestId
    }

    /**
     * Publishes the event to the web.
     */
    private fun publish(requestId: Int, event: Any) {
        val json = gson.toJson(gson.toJsonTree(event).asJsonObject.apply {
            addProperty("type", event::class.java.name)
        })

        webView.evaluateJavascript(
            "(function(conn){conn && conn.inbox && conn.inbox.publish([$requestId, $json])})(window.$connectionKey)",
            null
        )
    }
}
