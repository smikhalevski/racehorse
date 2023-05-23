package org.racehorse.utils

import com.google.gson.GsonBuilder
import org.junit.Assert
import org.junit.Test
import java.io.Serializable

class NaturalJsonAdapterTest {

    @Test
    fun testSerializesWebIntent() {
        val gson =
            GsonBuilder().serializeNulls().registerTypeAdapter(Serializable::class.java, NaturalJsonAdapter()).create()

        val value = WebIntent(extras = mapOf("aaa" to arrayOf(111, 222)))

        Assert.assertEquals(
            "{\"action\":null,\"type\":null,\"data\":null,\"flags\":0,\"extras\":{\"aaa\":[111,222]}}",
            gson.toJson(value)
        )
    }
}
