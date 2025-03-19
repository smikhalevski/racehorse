package org.racehorse.eventbus

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import org.racehorse.EventBridge
import org.racehorse.webview.RacehorseDownloadListener
import org.racehorse.webview.RacehorseWebChromeClient
import org.racehorse.webview.RacehorseWebViewClient
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations

/**
 * Marks an event that can be sent _from_ a page loaded in a [WebView].
 */
@Repeatable
annotation class Inbound(val eventType: String)

/**
 * Marks an event that can be sent _to_ a page loaded in a [WebView].
 */
annotation class Outbound(val eventType: String)

@SuppressLint("JavascriptInterface")
class RacehorseConnection(
    val webView: WebView,
    val json: Json = Json,
    block: (RacehorseConnection.() -> Unit)? = null
) {
    private companion object {
        const val TYPE_KEY = "type"
        const val PAYLOAD_KEY = "payload"
        const val JS_KEY = "racehorseConnection"

        val emptyJsonObject = JsonObject(mapOf())
    }

    val eventFlow = MutableSharedFlow<Any>()
    val requestEventCache = HashMap<String, KClass<*>>()
    val responseEventCache = HashMap<KClass<*>, String?>()
    val requestId = AtomicInteger()
    val listenerContexts = WeakHashMap<Any, ListenerContext>()

    val lifecycleScope by lazy { requireNotNull(webView.findViewTreeLifecycleOwner()?.lifecycleScope) }

    inner class ListenerContext(val requestId: Int?) {

        var responseEvent: Any? = null

        var isAsync = false

        fun respond(event: Any) {
            if (responseEvent == null && getOutboundEventType(event) != null) {
                responseEvent = event
            }

            listenerContexts[event] = this

            runBlocking {
                post(event)
            }
        }
    }

    val jsInterface = object {

        @JavascriptInterface
        fun post(eventJson: String): String {
            val eventJsonObject = json.parseToJsonElement(eventJson).jsonObject

            val eventType = eventJsonObject.getValue(TYPE_KEY).jsonPrimitive.content
            val eventClass = requireNotNull(requestEventCache[eventType]) { "No subscribers" }

            val event = json.decodeFromJsonElement(
                serializer(eventClass.java),
                eventJsonObject.getOrDefault(PAYLOAD_KEY, emptyJsonObject)
            )

            val listenerContext = ListenerContext(requestId.getAndIncrement())

            listenerContexts[event] = listenerContext

            runBlocking(lifecycleScope.coroutineContext) {
                eventFlow.emit(event)
            }

            listenerContext.responseEvent?.let {
                return@post stringifyEvent(it)
            }

            listenerContext.isAsync = true

            return listenerContext.requestId.toString()
        }
    }

    init {
        webView.webChromeClient = RacehorseWebChromeClient(this)
        webView.webViewClient = RacehorseWebViewClient(this)

        webView.setDownloadListener(RacehorseDownloadListener(this))
        webView.addJavascriptInterface(jsInterface, JS_KEY)

        block?.invoke(this)
    }

    /**
     * Registers an event listener.
     */
    inline fun <reified T> on(crossinline listener: suspend ListenerContext.(event: T) -> Unit) {
        T::class.findAnnotations<Inbound>().forEach { annotation ->
            val klass = requestEventCache.getOrPut(annotation.eventType) { T::class }

            check(klass == T::class) { "Classes ${T::class.java.name} and ${klass.java.name} should have distinct event types but both are $annotation" }
        }

        lifecycleScope.launch {
            eventFlow.collect { event ->
                if (event::class === T::class) {
                    coroutineContext.ensureActive()

                    listener.invoke(listenerContexts.getOrElse(event) { ListenerContext(null) }, event as T)
                }
            }
        }
    }

    fun post(event: Any) {
        val eventType = getOutboundEventType(event)
        val listenerContext = listenerContexts[event]

        if (eventType == null) {
            runBlocking {
                eventFlow.emit(event)
            }
            return
        }

        val requestId = listenerContext?.requestId ?: -1;

        if (requestId != -1 && listenerContext?.isAsync == false) {
            // Sync response will be sent
            return
        }

        val js = "(function(conn){" +
            "conn && conn.inbox && conn.inbox.publish([$requestId, ${stringifyEvent(event)}])" +
            "})(window.$JS_KEY)"

        webView.evaluateJavascript(js, null)
    }


    private fun stringifyEvent(event: Any) = "{" +
        "\"${EventBridge.TYPE_KEY}\":\"${event::class.java.name}\"," +
        "\"${EventBridge.PAYLOAD_KEY}\":${
            json.encodeToString(serializer(event::class.java), event)
        }}"

    private fun getOutboundEventType(event: Any) =
        responseEventCache.getOrPut(event::class) { event::class.findAnnotation<Outbound>()?.eventType }
}
