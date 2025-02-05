package org.racehorse.eventbus

import android.webkit.JavascriptInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import kotlin.coroutines.coroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation

@Repeatable
annotation class Inbound(val type: String)

annotation class Outbound

class EventBus {

    val events = MutableSharedFlow<Any>()
    val eventClasses = HashMap<String, KClass<*>>()

    suspend fun post(event: Any) {
        events.emit(event)
    }

    suspend inline fun <reified T> on(crossinline listener: suspend (event: T) -> Unit) {
        T::class.findAnnotations<Inbound>().forEach { annotation ->
            eventClasses[annotation.type] = T::class
        }

        events.filterIsInstance<T>().collectLatest { event ->
            coroutineContext.ensureActive()
            listener(event)
        }
    }
}

class JavascriptEventBus(
    val eventBus: EventBus,
    val json: Json = Json,
) {
    private companion object {
        const val TYPE_KEY = "type"
        const val PAYLOAD_KEY = "payload"

        val emptyJsonObject = JsonObject(mapOf())
    }

    init {
        eventBus.on<Any> { event ->
            if (event::class.hasAnnotation<Outbound>()) {
                // Send to WebView
            }
        }
    }

    @InternalSerializationApi
    @JavascriptInterface
    fun post(eventJson: String): String {
        val eventJsonObject = json.parseToJsonElement(eventJson).jsonObject

        val eventType = eventJsonObject.getValue(TYPE_KEY).jsonPrimitive.content
        val eventClass = eventBus.eventClasses[eventType] ?: throw UnsupportedOperationException("No subscribers")

        val event = json.decodeFromJsonElement(
            eventClass.serializer(),
            eventJsonObject.getOrDefault(PAYLOAD_KEY, emptyJsonObject)
        )

        runBlocking(Dispatchers.Default) {
            eventBus.post(event)
        }

        return "true"
    }
}

@Inbound("foo")
@Inbound("qqq")
@Serializable
data class FooEvent(val xxx: String)
