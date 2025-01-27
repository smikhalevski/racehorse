@file:UseSerializers(AnySerializer::class)

package org.racehorse.utils

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.Test

@Serializable
class Data(val a: Int, val b: Any?)

@Serializable
class Qqq(val xxx: Int)

class SerializationTest {

    @OptIn(InternalSerializationApi::class)
    @Test
    fun test() {
        val json = Json.encodeToString(Data(42, Qqq(222)))

        println(json)

        val jsonElement = Json.parseToJsonElement(json)

        val cls = Class.forName("org.racehorse.utils.Data")

        val obj = Json.decodeFromJsonElement(cls.kotlin.serializer(), jsonElement)

        println(obj)
    }
}
