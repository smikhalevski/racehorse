package org.racehorse.serializers

import android.content.Intent
import android.net.Uri
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.racehorse.utils.toBundle
import org.racehorse.utils.toMap

@Serializable
private class IntentSurrogate(
    var action: String? = null,
    val type: String? = null,
    var data: @Contextual Uri? = null,
    var flags: Int = 0,
    val selector: @Contextual Intent? = null,
    var extras: Map<String, @Contextual Any?>? = null,
    var categories: Set<String>? = null,
)

object IntentSerializer : KSerializer<Intent> {

    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor =
        SerialDescriptor("org.racehorse.serializers.IntentSerializer", IntentSurrogate.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: Intent) {
        encoder.encodeSerializableValue(
            IntentSurrogate.serializer(),
            IntentSurrogate(
                action = value.action,
                type = value.type,
                data = value.data,
                flags = value.flags,
                selector = value.selector,
                extras = value.extras?.toMap(),
                categories = value.categories,
            )
        )
    }

    override fun deserialize(decoder: Decoder): Intent =
        decoder.decodeSerializableValue(IntentSurrogate.serializer()).run {
            Intent(action).also { intent ->
                intent.setDataAndType(data, type)

                intent.flags = flags
                intent.selector = selector

                extras?.toBundle()?.let(intent::putExtras)

                categories?.forEach(intent::addCategory)
            }
        }
}
