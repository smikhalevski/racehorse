package org.racehorse.serializers

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
private class ThrowableSurrogate(
    val name: String,
    val message: String,
    val stack: String,
    val cause: @Contextual Throwable?
)

object ThrowableSerializer : KSerializer<Throwable> {
    @ExperimentalSerializationApi
    override val descriptor =
        SerialDescriptor("org.racehorse.serializers.ThrowableSerializer", ThrowableSurrogate.serializer().descriptor)

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
