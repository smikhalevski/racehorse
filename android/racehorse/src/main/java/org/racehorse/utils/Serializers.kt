package org.racehorse.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

fun CompositeDecoder.decodeNullableStringElement(descriptor: SerialDescriptor, index: Int): String? =
    decodeNullableSerializableElement(descriptor, index, String.serializer().nullable)

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
