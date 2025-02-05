@file:UseSerializers(AnySerializer::class)

package org.racehorse.serializers

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IntentSerializerTest {

    val json = Json {
        serializersModule = SerializersModule {
            contextual(IntentSerializer)
        }
    }

    @Test
    fun testSerializesIntent() {
        val intentStr = json.encodeToString(
            IntentSerializer,

            Intent("aaa").apply {
                data = Uri.parse("http://xxx.yyy")
                selector = Intent("xxx")

                putExtra("bbb",
                    Intent("ccc").apply {
                        putExtra("ddd", 111)
                    }
                )
            }
        )

        assertEquals(
            """{"action":"aaa","data":"http://xxx.yyy","selector":{"action":"xxx"},"extras":{"bbb":{"action":"ccc","extras":{"ddd":111}}}}""",
            intentStr
        )
    }

    @Test
    fun testDeserializesIntent() {
        val intentStr = """
          {
            "action": "aaa",
            "data": "http://xxx.yyy",
            "selector": {
              "action": "xxx"
            },
            "extras": {
              "bbb": [
                {
                  "__javaClass": "android.content.Intent",
                  "action": "ccc",
                  "extras": {
                    "ddd": 111
                  }
                }
              ]
            }
          }
          """

        val intent = json.decodeFromString(IntentSerializer, intentStr)

        assertEquals("http://xxx.yyy", intent.data?.toString())
        assertEquals("ccc", intent.extras?.getParcelableArrayList<Intent>("bbb")?.get(0)?.action)
        assertEquals(111.0, intent.extras?.getParcelableArrayList<Intent>("bbb")?.get(0)?.extras?.getDouble("ddd"))
    }
}
