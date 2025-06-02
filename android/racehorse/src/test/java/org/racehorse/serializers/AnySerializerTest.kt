package org.racehorse.serializers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import org.junit.Assert
import org.junit.Test

@Serializable
class Aaa(val xxx: Int)

@Serializable
class Bbb(val zzz: String)

class AnySerializerTest {

    @Test
    fun encodesKnownClasses() {
        Assert.assertEquals(
            """{"xxx":111,"yyy":222}""",
            encodeToString(AnySerializer(), mapOf("xxx" to 111, "yyy" to 222))
        )

        Assert.assertEquals("[111,222]", encodeToString(AnySerializer(), setOf(111, 222)))
        Assert.assertEquals("[111,222]", encodeToString(AnySerializer(), arrayOf(111, 222)))

        Assert.assertEquals(
            """{"first":111,"second":222,"third":333}""",
            encodeToString(AnySerializer(), Triple(111, 222, 333))
        )

        Assert.assertEquals(
            """{"first":"xxx","second":111}""",
            encodeToString(AnySerializer(), "xxx" to 111)
        )

        Assert.assertEquals("true", encodeToString(AnySerializer(), true))
    }

    @Test
    fun encodesUnknownAsNull() {
        Assert.assertEquals("null", encodeToString(AnySerializer(), object {}))
        Assert.assertEquals("[null,null]", encodeToString(AnySerializer(), setOf(object {}, object {})))
    }

    @Test
    fun encodesSerializable() {
        Assert.assertEquals("""{"xxx":111}""", encodeToString(AnySerializer(), Aaa(111)))
    }

    @Test
    fun encodesJavaClassProperty() {
        Assert.assertEquals(
            """{"xxx":111,"__javaClass":"org.racehorse.serializers.Aaa"}""",
            encodeToString(AnySerializer(isClassNameSerialized = true), Aaa(111))
        )

        Assert.assertEquals(
            """{"xxx":111,"aaa":"org.racehorse.serializers.Aaa"}""",
            encodeToString(AnySerializer(javaClassKey = "aaa", isClassNameSerialized = true), Aaa(111))
        )
    }

    @Test
    fun encodingThrowsIfInternalPropertyIsRedefined() {
        Assert.assertThrows(IllegalArgumentException::class.java) {
            encodeToString(AnySerializer(javaClassKey = "zzz", isClassNameSerialized = true), Bbb(""))
        }
    }

    @Test
    fun decodesPrimitives() {
        Assert.assertEquals(decodeFromString(AnySerializer(), """"xxx""""), "xxx")
        Assert.assertEquals(decodeFromString(AnySerializer(), "111"), 111.0)

        Assert.assertThrows(IllegalArgumentException::class.java) {
            decodeFromString(AnySerializer(), "null")
        }
    }

    @Test
    fun decodesObjectAsMap() {
        val result = decodeFromString(AnySerializer(), """{"xxx":111,"yyy":222}""") as Map<*, *>

        Assert.assertEquals(result.size, 2)
        Assert.assertEquals(result["xxx"], 111.0)
        Assert.assertEquals(result["yyy"], 222.0)
    }

    @Test
    fun decodesArrayAsList() {
        val result = decodeFromString(AnySerializer(), "[111,222]") as List<*>

        Assert.assertEquals(result.size, 2)
        Assert.assertEquals(result[0], 111.0)
        Assert.assertEquals(result[1], 222.0)
    }

    @Test
    fun decodesUsingJavaClassProperty() {
        val result =
            decodeFromString(AnySerializer(), """{"__javaClass":"org.racehorse.serializers.Aaa","xxx":111}""") as Aaa

        Assert.assertEquals(result.xxx, 111)
    }

    @Test
    fun decodingThrowsClassNotFoundException() {
        Assert.assertThrows(ClassNotFoundException::class.java) {
            decodeFromString(AnySerializer(), """{"__javaClass":"zzz","xxx":111}""")
        }
    }
}
