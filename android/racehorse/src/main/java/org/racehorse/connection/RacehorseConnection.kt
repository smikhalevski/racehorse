package org.racehorse.connection

import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.serializer
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

@Target(AnnotationTarget.CLASS)
annotation class Message(
    val type: String,
    val threadMode: ThreadMode = ThreadMode.ASYNC_BACKGROUND
)

enum class ThreadMode {
    /**
     * Message handlers are run synchronously on the [JavascriptInterface] thread, blocking it until the handler returns.
     *
     * **Handler must complete quickly.** Slow handlers stall every JS call queued behind this one on the bridge thread.
     */
    SYNC,

    /**
     * Message handlers are run on the Android main (UI) thread.
     */
    ASYNC_UI,

    /**
     * Message handlers are run on an IO-optimised thread pool.
     */
    ASYNC_IO,

    /**
     * Message handlers are run on the default CPU-bound thread pool.
     */
    ASYNC_BACKGROUND,
}

private const val TYPE_KEY = "type"

private const val PAYLOAD_KEY = "payload"

/**
 * Bridges typed Kotlin messages with a [WebView] JS runtime.
 *
 * Lifecycle:
 * 1. Call [connect] when a [WebView] becomes available.
 * 2. Call [disconnect] when the [WebView] is being destroyed; this cancels all in-flight work.
 * 3. [connect] may be called again after [disconnect] to reattach.
 */
class RacehorseConnection(
    private val json: Json = Json,
    private val connectionName: String = "racehorseConnection"
) {
    private val messageHandlers =
        ConcurrentHashMap<KClass<*>, CopyOnWriteArrayList<suspend MessageContext.(Any) -> Any?>>()

    private val messageRegistry = MessageRegistry().apply {
        register(VoidMessage::class)
        register(ExceptionMessage::class)
    }

    private val asyncRequestCount = AtomicLong()

    @Volatile
    private var webView: WebView? = null

    @Volatile
    private var coroutineScope: CoroutineScope? = null

    private val javascriptInterface = object {

        @JavascriptInterface
        fun post(messageJson: String): String {
            try {
                val messageObject = json.parseToJsonElement(messageJson).jsonObject

                val messageType = messageObject.getValue(TYPE_KEY).jsonPrimitive.content
                val messagePayload = messageObject.getOrElse(PAYLOAD_KEY) { JsonObject(mapOf()) }

                val metadata =
                    requireNotNull(messageRegistry.getMetadata(messageType)) { "Unknown message type \"$messageType\"" }

                val message = json.decodeFromJsonElement(serializer(metadata.messageClass.java), messagePayload)

                val coroutineContext = when (metadata.threadMode) {
                    ThreadMode.SYNC -> return runBlocking { encodeMessage(handleMessage(message) ?: VoidMessage) }
                    ThreadMode.ASYNC_IO -> Dispatchers.IO
                    ThreadMode.ASYNC_UI -> Dispatchers.Main
                    ThreadMode.ASYNC_BACKGROUND -> Dispatchers.Default
                }

                val requestId = asyncRequestCount.incrementAndGet()

                requireNotNull(coroutineScope) { "Not connected" }.launch(coroutineContext) {
                    try {
                        postMessage(requestId, handleMessage(message) ?: VoidMessage)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        postMessage(requestId, ExceptionMessage(e))
                    }
                }

                return requestId.toString()
            } catch (e: Throwable) {
                return encodeMessage(ExceptionMessage(e))
            }
        }
    }

    fun supportsMessage(messageClass: KClass<*>): Boolean = messageRegistry.getMetadata(messageClass) != null

    fun supportsMessage(messageType: String): Boolean = messageRegistry.getMetadata(messageType) != null

    @MainThread
    fun connect(view: WebView) {
        disconnect()

        webView = view.apply { addJavascriptInterface(javascriptInterface, connectionName) }
        coroutineScope = CoroutineScope(SupervisorJob())
    }

    @MainThread
    fun disconnect() {
        coroutineScope?.cancel()
        coroutineScope = null

        webView?.removeJavascriptInterface(connectionName)
        webView = null
    }

    inline fun <reified T : Any> on(noinline handler: suspend MessageContext.(T) -> Any?) = on(T::class, handler)

    fun <T : Any> on(messageClass: KClass<T>, handler: suspend MessageContext.(T) -> Any?) {
        messageRegistry.register(messageClass)

        messageHandlers.computeIfAbsent(messageClass) { CopyOnWriteArrayList() }.add { message ->
            require(messageClass.isInstance(message))
            @Suppress("UNCHECKED_CAST")
            handler(message as T)
        }
    }

    suspend fun notify(message: Any) {
        postMessage(null, message)
    }

    private suspend fun postMessage(requestId: Long?, message: Any) {
        withContext(Dispatchers.Main) {
            val messageJson = encodeMessage(message)

            webView?.evaluateJavascript(
                """
                    (function(conn){
                      conn && conn.inbox && conn.inbox.publish([$requestId, $messageJson])
                    })(window[${json.encodeToString(connectionName)}])
                """.trimIndent(),
                null
            )
        }
    }

    private suspend fun <T : Any> handleMessage(event: T): Any? {
        val handlers = requireNotNull(messageHandlers[event::class]?.takeIf { it.isNotEmpty() }) {
            "No handlers for ${event::class.simpleName}"
        }

        val messageContext = MessageContextImpl(messageRegistry)
        val errors = ArrayList<Throwable>()

        handlers.forEach { handler ->
            try {
                messageContext.handler(event)?.takeUnless { it == Unit }?.let(messageContext::setResponseMessage)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                errors.add(e)
            }
        }

        if (errors.isNotEmpty()) {
            throw errors.first().apply {
                errors.drop(1).forEach(::addSuppressed)
            }
        }

        return messageContext.responseMessage

    }

    private fun encodeMessage(message: Any): String =
        buildJsonObject {
            put(TYPE_KEY, messageRegistry.register(message::class).messageType)
            put(PAYLOAD_KEY, json.encodeToJsonElement(serializer(message::class.java), message))
        }.toString()
}

@Message("racehorse.void")
@Serializable
private object VoidMessage

@Message("racehorse.exception")
@Serializable
private class ExceptionMessage(val name: String?, val message: String?, val stack: String) {
    constructor(e: Throwable) : this(e::class.qualifiedName ?: "Exception", e.message, e.stackTraceToString())
}

interface MessageContext {
    val hasResponse: Boolean
}

private class MessageContextImpl(val messageRegistry: MessageRegistry) : MessageContext {
    private val _responseMessage = AtomicReference<Any?>(null)

    val responseMessage: Any? get() = _responseMessage.get()

    override val hasResponse get() = responseMessage != null

    fun setResponseMessage(message: Any) {
        messageRegistry.register(message::class)

        check(_responseMessage.compareAndSet(null, message)) {
            "Message already has a response"
        }
    }
}

private class MessageRegistry {
    private val metadataByType = ConcurrentHashMap<String, MessageMetadata>()

    private val metadataByClass = ConcurrentHashMap<KClass<*>, MessageMetadata>()

    fun register(messageClass: KClass<*>): MessageMetadata {
        return metadataByClass.computeIfAbsent(messageClass) {
            val message = requireNotNull(messageClass.findAnnotation<Message>()) {
                "${messageClass.simpleName} is not a message"
            }

            val metadata = metadataByType.computeIfAbsent(message.type) {
                MessageMetadata(messageClass, message.type, message.threadMode)
            }

            require(metadata.messageClass == messageClass) {
                "Message type \"${message.type}\" used by both ${metadata.messageClass} and ${messageClass.simpleName}"
            }

            metadata
        }
    }

    fun getMetadata(messageClass: KClass<*>): MessageMetadata? = metadataByClass[messageClass]

    fun getMetadata(messageType: String): MessageMetadata? = metadataByType[messageType]
}

private class MessageMetadata(
    val messageClass: KClass<*>,
    val messageType: String,
    val threadMode: ThreadMode = ThreadMode.ASYNC_BACKGROUND
)
