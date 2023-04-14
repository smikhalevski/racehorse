package org.racehorse

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.greenrobot.eventbus.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * An event posted from the web view. Only events that implement this interface are allowed to pass trough the event
 * bridge.
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
 *
 * @param ok Indicates that the response is successful or not.
 */
abstract class ResponseEvent(val ok: Boolean = true) : ChainableEvent()

/**
 * Response with no payload.
 */
class VoidEvent : ResponseEvent()

/**
 * Response that describes an occurred exception.
 */
class ExceptionEvent(@Transient val cause: Throwable) : ResponseEvent(false) {
    val stackTrace = cause.stackTraceToString()
}

/**
 * The event bridge enables communication between the app in the web view and Android-native code.
 *
 * @param webView The [WebView] to which the event bridge will add the connection Javascript interface.
 * @param eventBus The event bus to which events are posted.
 * @param gson The [Gson] instance that is used for event serialization.
 * @param connectionKey The key of the `window` that exposes the connection Javascript interface.
 */
open class EventBridge(
    private val webView: WebView,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val gson: Gson = GsonBuilder().serializeNulls().create(),
    private val connectionKey: String = "racehorseConnection"
) {

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
