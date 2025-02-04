package org.racehorse.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MapKeySerializer : KSerializer<Any> {
    override val descriptor =
        PrimitiveSerialDescriptor("org.racehorse.serializers.MapKeySerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Any) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Any = decoder.decodeString()
}