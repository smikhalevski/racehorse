package org.racehorse.serializers

import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import org.junit.Assert
import org.junit.Test
import java.io.File

class FileSerializerTest {

    @Test
    fun encodesFile() {
        Assert.assertEquals(""""file:///xxx/yyy"""", encodeToString(FileSerializer, File("/xxx/yyy")))
    }

    @Test
    fun decodesFile() {
        Assert.assertEquals("/xxx/yyy", decodeFromString(FileSerializer, """"file:///xxx/yyy"""").absolutePath)
    }
}
