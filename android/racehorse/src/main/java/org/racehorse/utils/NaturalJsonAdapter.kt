package org.racehorse.utils

import android.os.Bundle
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.Date

/**
 * Gson adapter that serializes and deserializes any values.
 */
class NaturalJsonAdapter : JsonDeserializer<Any?>, JsonSerializer<Any?> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Any? =
        when (getType(typeOfT)) {
            Pair::class.java -> Pair<Any, Any>(
                context.deserialize(json.asJsonArray.get(0), getTypeArgument(typeOfT, 0)),
                context.deserialize(json.asJsonArray.get(1), getTypeArgument(typeOfT, 1)),
            )

            else -> deserializeAny(json, typeOfT, context)
        }

    override fun serialize(src: Any?, typeOfSrc: Type, context: JsonSerializationContext) = when (src) {

        is Number -> JsonPrimitive(src)

        is String -> JsonPrimitive(src)

        is Boolean -> JsonPrimitive(src)

        is Date -> JsonPrimitive(src.time)

        is Char -> JsonPrimitive(src)

        is Array<*> -> serializeArray(src.asIterable(), src::class.java.componentType, context)

        is Map<*, *> -> serializeObject(src.toList(), context)

        is Bundle -> serializeObject(src.keySet().map {
            @Suppress("DEPRECATION")
            it to src.get(it)
        }, context)

        is Pair<*, *> -> JsonArray().apply {
            add(context.serialize(src.first))
            add(context.serialize(src.second))
        }

        is Iterable<*> -> serializeArray(src, getTypeArgument(typeOfSrc, 0), context)

        else -> null
    }

    private fun getType(type: Type): Type =
        (type as? ParameterizedType)?.rawType ?: type

    private fun getTypeArgument(type: Type, index: Int): Type =
        (type as? ParameterizedType)?.actualTypeArguments?.get(index) ?: Any::class.java

    private fun deserializeAny(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Any? = when {

        json.isJsonObject -> json.asJsonObject.run {
            keySet().associateWithTo(LinkedHashMap<String, Any>()) {
                context.deserialize(get(it), Any::class.java)
            }
        }

        json.isJsonArray -> json.asJsonArray.run {
            map { context.deserialize<Any>(it, Any::class.java) }
        }

        json.isJsonPrimitive -> deserializePrimitive(json.asJsonPrimitive, typeOfT)

        else -> null
    }

    private fun deserializePrimitive(json: JsonPrimitive, typeOfT: Type): Any = when {

        json.isString -> json.asString

        json.isBoolean -> json.asBoolean

        else -> {
            val value = try {
                json.asBigDecimal.longValueExact()
            } catch (_: ArithmeticException) {
                json.asDouble
            }
            if (typeOfT == Date::class.java) {
                Date(value.toLong())
            } else {
                value
            }
        }
    }

    private fun serializeArray(src: Iterable<*>, typeOfSrc: Type, context: JsonSerializationContext) =
        JsonArray().apply {
            src.forEach { value ->
                add(context.serialize(value, typeOfSrc))
            }
        }

    private fun serializeObject(src: Iterable<Pair<*, *>>, context: JsonSerializationContext) = JsonObject().apply {
        src.forEach { (key, value) ->
            add(key.toString(), value?.let { context.serialize(value, value::class.java) })
        }
    }
}
