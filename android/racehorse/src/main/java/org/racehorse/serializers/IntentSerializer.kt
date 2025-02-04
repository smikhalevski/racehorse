@file:UseSerializers(IntentSerializer::class, UriSerializer::class, AnySerializer::class)

package org.racehorse.serializers

import android.content.Intent
import android.net.Uri
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.racehorse.utils.toMap

object IntentSerializer : KSerializer<Intent> {

    @ExperimentalSerializationApi
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

    override fun deserialize(decoder: Decoder): Intent {
        TODO()
    }
}

@Serializable
private class IntentSurrogate(
    var action: String? = null,
    val type: String? = null,
    var data: Uri? = null,
    var flags: Int = 0,
    val selector: Intent? = null,
    var extras: Map<String, Any?>? = null,
    var categories: Set<String>? = null,
)
