@file:OptIn(ExperimentalSerializationApi::class)

package org.racehorse.utils

import android.net.Uri
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import java.io.File

fun CompositeDecoder.decodeNullableStringElement(descriptor: SerialDescriptor, index: Int): String? =
    decodeNullableSerializableElement(descriptor, index, String.serializer().nullable)

fun CompositeDecoder.decodeNullableIntElement(descriptor: SerialDescriptor, index: Int): Int? =
    decodeNullableSerializableElement(descriptor, index, Int.serializer().nullable)


object AnySerializer : KSerializer<Any?> {
    private val classCache = HashMap<String, Class<*>>()

    override val descriptor = buildClassSerialDescriptor("Any") {
        element("type", serialDescriptor<String>())
        element("payload", buildClassSerialDescriptor("AnyPayload"))
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: Any?) {
        value ?: return encoder.encodeNull()

        when (value) {
            is String -> encoder.encodeString(value)
            is Number -> encoder.encodeDouble(value.toDouble())
            is Boolean -> encoder.encodeBoolean(value)
            else -> encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value::class.java.name)
                encodeSerializableElement(descriptor, 1, value::class.serializer() as KSerializer<Any?>, value)
            }
        }
    }

    @OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Any? {
        try {
            return decoder.decodeNull()
        } catch (_: Throwable) {
        }

        try {
            return decoder.decodeString()
        } catch (_: Throwable) {
        }

        try {
            return decoder.decodeDouble()
        } catch (_: Throwable) {
        }

        try {
            return decoder.decodeBoolean()
        } catch (_: Throwable) {
        }

        return decoder.decodeStructure(descriptor) {
            val className = decodeStringElement(descriptor, 0)

            val clazz = classCache.getOrPut(className) {
                try {
                    Class.forName(className)
                } catch (_: ClassNotFoundException) {
                    throw IllegalArgumentException("Unrecognized type")
                }
            }

            decodeSerializableElement(descriptor, 1, clazz::class.serializer())
        }
    }
}

object UriSerializer : KSerializer<Uri> {
    override val descriptor = buildClassSerialDescriptor("Uri")

    override fun serialize(encoder: Encoder, value: Uri) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Uri {
        return Uri.parse(decoder.decodeString())
    }
}

object FileSerializer : KSerializer<File> {
    override val descriptor = buildClassSerialDescriptor("File")

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(decoder: Decoder): File {
        return File(decoder.decodeString())
    }
}

object ThrowableSerializer : KSerializer<Throwable> {
    override val descriptor = buildClassSerialDescriptor("Throwable")

    override fun serialize(encoder: Encoder, value: Throwable) {
        encoder.encodeString(value.stackTraceToString())
    }

    override fun deserialize(decoder: Decoder): Throwable {
        return Throwable(decoder.decodeString())
    }
}
