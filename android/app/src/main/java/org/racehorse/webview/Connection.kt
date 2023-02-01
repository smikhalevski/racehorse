package org.racehorse.webview

import android.webkit.JavascriptInterface
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.events.ErrEvent
import org.racehorse.webview.events.WebEvent

/**
 * The connection injected to the web page.
 */
class Connection(private val gson: Gson, private val eventBus: EventBus) {

    @JavascriptInterface
    fun post(requestId: Long, eventStr: String) {
        try {
            val jsonObject = gson.fromJson(eventStr, JsonObject::class.java)
            val type = jsonObject["type"].asString

            jsonObject.remove("type")
            jsonObject.addProperty("requestId", requestId)

            val eventClass = Class.forName(type)

            if (!eventClass.isAssignableFrom(WebEvent::class.java)) {
                throw IllegalArgumentException("Cannot use class $type as the request event")
            }

            val event = gson.fromJson(jsonObject, eventClass)

            eventBus.post(event)
        } catch (e: Throwable) {
            eventBus.post(ErrEvent(requestId, e))
        }
    }
}
