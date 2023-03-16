package org.racehorse.webview

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus

/**
 * The connection injected to the web page.
 */
internal class Connection(private val gson: Gson, private val eventBus: EventBus) {

    @JavascriptInterface
    fun post(requestId: Int, eventData: String) {
        val event = try {
            val jsonObject = gson.fromJson(eventData, JsonObject::class.java)
            val eventClass = Class.forName(jsonObject["type"].asString)

            jsonObject.remove("requestId")
            jsonObject.remove("type")

            if (!InboxEvent::class.java.isAssignableFrom(eventClass)) {
                throw IllegalArgumentException("Expected an event class but found $eventClass")
            }

            gson.fromJson(jsonObject, eventClass)
        } catch (throwable: Throwable) {
            eventBus.post(ExceptionResponseEvent(throwable).setRequestId(requestId))
            return
        }
        if (event is ChainableEvent) {
            eventBus.post(event.setRequestId(requestId))
            return
        }

        eventBus.post(event)
        eventBus.post(VoidResponseEvent().setRequestId(requestId))
    }
}
