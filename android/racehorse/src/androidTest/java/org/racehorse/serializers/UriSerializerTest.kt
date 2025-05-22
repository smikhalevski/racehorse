package org.racehorse.serializers

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class UriSerializerTest {

    @Test
    fun encodesUri() {
        Assert.assertEquals(""""/xxx/yyy"""", encodeToString(UriSerializer, Uri.parse("/xxx/yyy")))
    }

    @Test
    fun decodesUri() {
        Assert.assertEquals("/xxx/yyy", decodeFromString(UriSerializer, """"/xxx/yyy"""").toString())
    }
}
