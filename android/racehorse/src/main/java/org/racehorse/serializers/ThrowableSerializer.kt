package org.racehorse.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ThrowableSerializer : KSerializer<Throwable> {
    override val descriptor = buildClassSerialDescriptor("org.racehorse.serializers.ThrowableSerializer")

    override fun serialize(encoder: Encoder, value: Throwable) = encoder.encodeString(value.stackTraceToString())

    override fun deserialize(decoder: Decoder): Throwable = Throwable(decoder.decodeString())
}
