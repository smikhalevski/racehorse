package org.racehorse.serializers

import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.File

object FileSerializer : KSerializer<File> {
    override val descriptor = buildClassSerialDescriptor("org.racehorse.serializers.FileSerializer")

    override fun serialize(encoder: Encoder, value: File) = encoder.encodeString(value.toUri().toString())

    override fun deserialize(decoder: Decoder): File = Uri.parse(decoder.decodeString()).toFile()
}
