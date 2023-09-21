package org.racehorse.utils

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
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
            "{\"action\":null,\"type\":null,\"data\":null,\"flags\":0,\"selector\":null,\"extras\":{\"aaa\":[111,222]},\"categories\":null}",
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

    @Test
    fun testSerializesPair() {
        val gson = GsonBuilder().registerTypeAdapter(Pair::class.java, NaturalJsonAdapter()).create()

        Assert.assertEquals("[111,\"aaa\"]", gson.toJson(111 to "aaa"))
    }

    @Test
    fun testSerializesArrayOfPairs() {
        val gson = GsonBuilder().registerTypeAdapter(Pair::class.java, NaturalJsonAdapter()).create()

        Assert.assertEquals("[[111,\"aaa\"]]", gson.toJson(arrayOf(111 to "aaa")))
    }

    @Test
    fun testDeserializesPair() {
        val gson = GsonBuilder().registerTypeAdapter(Pair::class.java, NaturalJsonAdapter()).create()

        val result = gson.fromJson("[111,\"aaa\"]", Pair::class.java)

        Assert.assertEquals(111.0, result.first)
        Assert.assertEquals("aaa", result.second)
    }

    @Test
    fun testDeserializesArrayOfPairs() {
        val gson = GsonBuilder()
            .registerTypeAdapter(Array::class.java, NaturalJsonAdapter())
            .registerTypeAdapter(Pair::class.java, NaturalJsonAdapter())
            .create()

        val result = gson.fromJson<Array<Pair<Int, String>>>(
            "[[111,\"aaa\"]]",
            object : TypeToken<Array<Pair<Int, String>>>() {}.type
        )

        Assert.assertEquals(111, result[0].first)
        Assert.assertEquals("aaa", result[0].second)
    }
}
