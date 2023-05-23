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
import java.lang.reflect.Type

/**
 * Gson adapter that serializes and deserializes any values.
 */
class NaturalJsonAdapter : JsonDeserializer<Any?>, JsonSerializer<Any?> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Any? = when {

        json.isJsonObject -> json.asJsonObject.run {
            keySet().associateWithTo(LinkedHashMap<String, Any>()) {
                context.deserialize(get(it), Any::class.java)
            }
        }

        json.isJsonArray -> json.asJsonArray.run {
            foldIndexed(arrayOfNulls<Any>(size())) { index, array, json ->
                array[index] = context.deserialize(json, Any::class.java)
                array
            }
        }

        json.isJsonPrimitive -> deserializePrimitive(json.asJsonPrimitive)

        else -> null
    }

    private fun deserializePrimitive(json: JsonPrimitive): Any = when {

        json.isString -> json.asString

        json.isBoolean -> json.asBoolean

        else -> try {
            json.asBigDecimal.longValueExact()
        } catch (_: ArithmeticException) {
            json.asDouble
        }
    }

    override fun serialize(src: Any?, typeOfSrc: Type, context: JsonSerializationContext) = when (src) {

        is Number -> JsonPrimitive(src)

        is String -> JsonPrimitive(src)

        is Boolean -> JsonPrimitive(src)

        is Char -> JsonPrimitive(src)

        is Array<*> -> serializeArray(src.asIterable(), context)

        is Collection<*> -> serializeArray(src, context)

        is Map<*, *> -> serializeObject(src.toList(), context)

        is Bundle -> serializeObject(src.keySet().map {
            @Suppress("DEPRECATION")
            it to src.get(it)
        }, context)

        else -> null
    }

    private fun serializeArray(src: Iterable<*>, context: JsonSerializationContext) = JsonArray().apply {
        src.forEach { value ->
            add(value?.let(context::serialize))
        }
    }

    private fun serializeObject(src: Iterable<Pair<*, *>>, context: JsonSerializationContext) = JsonObject().apply {
        src.forEach { (key, value) ->
            add(key.toString(), value?.let(context::serialize))
        }
    }
}
