@file:UseSerializers(ThrowableSerializer::class)

package org.racehorse.serializers

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private class ThrowableSurrogate(
    val name: String,
    val message: String,
    val stack: String,
    val cause: Throwable?
)

object ThrowableSerializer : KSerializer<Throwable> {
    override val descriptor = buildClassSerialDescriptor("org.racehorse.serializers.ThrowableSerializer")

    @InternalSerializationApi
    override fun serialize(encoder: Encoder, value: Throwable) {
        encoder.encodeSerializableValue(
            ThrowableSurrogate.serializer(),
            ThrowableSurrogate(
                name = value::class.java.simpleName,
                message = value.message.orEmpty(),
                stack = value.stackTraceToString(),
                cause = value.cause
            )
        )
    }

    override fun deserialize(decoder: Decoder) = throw UnsupportedOperationException()
}
