package org.racehorse.utils

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import kotlin.jvm.internal.Reflection
import kotlin.reflect.full.memberProperties

/**
 * Provides Kotlin-imposed null-safety checks during deserialization.
 */
class KotlinTypeAdapterFactory : TypeAdapterFactory {

    override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        if (type.rawType.declaredAnnotations.any { it.annotationClass.qualifiedName == "kotlin.Metadata" }) {
            return KotlinTypeAdapter(gson.getDelegateAdapter(this, type), type)
        }
        return null
    }
}

private class KotlinTypeAdapter<T>(val delegateAdapter: TypeAdapter<T>, val type: TypeToken<T>) : TypeAdapter<T>() {

    override fun write(out: JsonWriter, value: T?) = delegateAdapter.write(out, value)

    override fun read(input: JsonReader): T? {
        val value = delegateAdapter.read(input) ?: return null

        Reflection.createKotlinClass(type.rawType).memberProperties.forEach {
            if (!it.returnType.isMarkedNullable && it.get(value) == null) {
                throw JsonParseException("${it.name} cannot be null")
            }
        }

        return value
    }
}
