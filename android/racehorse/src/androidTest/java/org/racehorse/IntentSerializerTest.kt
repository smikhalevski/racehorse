@file:UseSerializers(AnySerializer::class)

package org.racehorse

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.Test
import org.junit.runner.RunWith
import org.racehorse.serializers.AnySerializer
import org.racehorse.serializers.IntentSerializer

@RunWith(AndroidJUnit4::class)
class IntentSerializerTest {

    @InternalSerializationApi
    @Test
    fun test() {
        MapSerializer(String.serializer(), String.serializer()).descriptor

        val json = Json {
            serializersModule = SerializersModule {
                contextual(IntentSerializer)
            }
        }

        val str = json.encodeToString(
            IntentSerializer,

            Intent("test111").apply {
                data = Uri.parse("http://xxx.yyy")
                selector = Intent("xxx")

                putExtra("another_intent",
                    Intent("test222").apply {
                        putExtra("aaa", 123)
                    }
                )
            }
        )

        json.decodeFromString(IntentSerializer, str)

        println(str)
    }
}

