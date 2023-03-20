package org.racehorse.webview

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.atomic.AtomicInteger

/**
 * The connection injected to the web page.
 */
internal class Connection(private val gson: Gson, private val eventBus: EventBus) {

    private var requestId = AtomicInteger()

    @JavascriptInterface
    fun post(eventJson: String): Int {
        val requestId = this.requestId.getAndIncrement()

        val event = try {
            val jsonObject = gson.fromJson(eventJson, JsonObject::class.java)
            val eventClass = Class.forName(jsonObject["type"].asString)

            jsonObject.remove("requestId")
            jsonObject.remove("type")

            require(InboxEvent::class.java.isAssignableFrom(eventClass)) { "Not an event: $eventClass" }

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
}
