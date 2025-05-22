package org.racehorse.serializers

import kotlinx.serialization.json.Json.Default.encodeToString
import org.junit.Assert
import org.junit.Test

class ThrowableSerializerTest {

    @Test
    fun encodesIllegalStateException() {
        Assert.assertTrue(
            encodeToString(
                ThrowableSerializer,
                IllegalStateException("expected")
            ).startsWith("""{"name":"IllegalStateException","message":"expected","stack":"java.lang.IllegalStateException: expected""")
        )
    }
}
