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
    fun post(requestId: Int, eventJson: String) {
        val event: Any

        try {
            val jsonObject = gson.fromJson(eventJson, JsonObject::class.java)
            jsonObject.remove("requestId")

            val eventType = jsonObject["type"].asString

            jsonObject.remove("type")

            val eventClass = Class.forName(eventType)

            if (!InboxEvent::class.java.isAssignableFrom(eventClass)) {
                throw IllegalArgumentException("Not an event $eventType")
            }

            event = gson.fromJson(jsonObject, eventClass)
        } catch (throwable: Throwable) {
            eventBus.post(ExceptionResponseEvent(throwable).chain(requestId))
            return
        }

        if (event is ChainableEvent) {
            eventBus.post(event.chain(requestId))
        } else {
            eventBus.post(VoidResponseEvent().chain(requestId))
        }
    }
}
