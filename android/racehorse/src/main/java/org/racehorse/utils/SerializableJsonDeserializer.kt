package org.racehorse.utils

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.io.Serializable
import java.lang.reflect.Type
import java.util.LinkedList

/**
 * Gson deserializer that converts objects to [LinkedHashMap] and arrays to [LinkedList].
 */
class SerializableJsonDeserializer : JsonDeserializer<Serializable?> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Serializable? = when {

        json.isJsonObject -> json.asJsonObject.run {
            keySet().associateWithTo(LinkedHashMap<String, Serializable>()) {
                context.deserialize(get(it), Serializable::class.java)
            }
        }

        json.isJsonArray -> json.asJsonArray.mapTo(LinkedList<Serializable>()) {
            context.deserialize(it, Serializable::class.java)
        }

        else -> null
    }
}
