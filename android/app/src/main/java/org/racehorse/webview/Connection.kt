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
    fun post(requestId: Int, eventStr: String) {
        val event: Any

        try {
            val jsonObject = gson.fromJson(eventStr, JsonObject::class.java)
            jsonObject.remove("requestId")

            val eventType = jsonObject["type"].asString

            jsonObject.remove("type")

            val eventClass = Class.forName(eventType)

            if (!InboxEvent::class.java.isAssignableFrom(eventClass)) {
                throw IllegalArgumentException("Not an event $eventType")
            }

            event = gson.fromJson(jsonObject, eventClass)
        } catch (throwable: Throwable) {
            eventBus.post(ErrorResponseEvent(requestId, throwable))
            return
        }

        if (event is ChainableEvent) {
            event.requestId = requestId
            eventBus.post(event)
        } else {
            eventBus.post(AutoCloseResponseEvent(requestId))
        }
    }
}
