package org.racehorse.utils

import com.google.gson.GsonBuilder
import org.junit.Assert
import org.junit.Test
import java.io.Serializable
import java.util.Date

class NaturalJsonAdapterTest {

    @Test
    fun testSerializesSerializableIntent() {
        val gson =
            GsonBuilder().serializeNulls().registerTypeAdapter(Serializable::class.java, NaturalJsonAdapter()).create()

        val value = SerializableIntent(extras = mapOf("aaa" to arrayOf(111, 222)))

        Assert.assertEquals(
            "{\"action\":null,\"type\":null,\"data\":null,\"flags\":0,\"extras\":{\"aaa\":[111,222]}}",
            gson.toJson(value)
        )
    }

    @Test
    fun testSerializesDate() {
        val gson = GsonBuilder().registerTypeAdapter(Date::class.java, NaturalJsonAdapter()).create()

        Assert.assertEquals("1686227164126", gson.toJson(Date(1686227164126)))
    }

    @Test
    fun testDeserializesDate() {
        val gson = GsonBuilder().registerTypeAdapter(Date::class.java, NaturalJsonAdapter()).create()

        Assert.assertEquals(1686227164126, gson.fromJson("1686227164126", Date::class.java).time)
    }
}
