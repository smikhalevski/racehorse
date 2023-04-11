package org.racehorse

import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.greenrobot.eventbus.*
import java.util.concurrent.atomic.AtomicInteger

const val CONNECTION_KEY = "racehorseConnection"

open class ConnectionController(
    private val webView: WebView,
    private val eventBus: EventBus = EventBus.getDefault(),
    private val gson: Gson = GsonBuilder().serializeNulls().create()
) {

    private var requestId = AtomicInteger()

    fun start() {
        webView.addJavascriptInterface(this, CONNECTION_KEY)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onResponse(event: ResponseEvent) {
        require(event.requestId >= 0) { "Expected a request ID to be set for a response event" }

        publishEvent(event.requestId, event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onOutbound(event: OutboundEvent) {
        publishEvent(-1, event)
    }

    @Subscribe
    fun onNoSubscriber(event: NoSubscriberEvent) {
        (event.originalEvent as? RequestEvent)?.let {
            eventBus.post(ExceptionResponseEvent(IllegalStateException("No subscribers for $it")).setRequestId(it.requestId))
        }
    }

    @Subscribe
    fun onSubscriberException(event: SubscriberExceptionEvent) {
        when (val causingEvent = event.causingEvent) {

            is ExceptionResponseEvent -> causingEvent.cause.printStackTrace()

            is RequestEvent -> eventBus.post(ExceptionResponseEvent(event.throwable).setRequestId(causingEvent.requestId))
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

            require(InboundEvent::class.java.isAssignableFrom(eventClass)) { "Not an event: $eventClass" }

            gson.fromJson(jsonObject, eventClass)
        } catch (throwable: Throwable) {
            eventBus.post(ExceptionResponseEvent(throwable).setRequestId(requestId))
            return requestId
        }

        if (event is ChainableEvent) {
            eventBus.post(event.setRequestId(requestId))
            return requestId
        }

        eventBus.post(event)
        eventBus.post(VoidResponseEvent().setRequestId(requestId))
        return requestId
    }

    /**
     * Publishes the event to the web.
     */
    private fun publishEvent(requestId: Int, event: Any) {
        val json = gson.toJson(gson.toJsonTree(event).asJsonObject.also {
            it.addProperty("type", event::class.java.name)
        })

        webView.evaluateJavascript(
            "(function(conn){conn && conn.inbox && conn.inbox.publish([$requestId, $json])})(window.$CONNECTION_KEY)",
            null
        )
    }
}
