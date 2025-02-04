package org.racehorse.utils

import android.os.Bundle
import java.io.Serializable

fun Bundle.toMap(): Map<String, Any?> = buildMap {
    for (key in keySet()) {
        @Suppress("DEPRECATION")
        put(key, this@toMap.get(key))
    }
}

fun Map<String, Serializable>.toBundle(): Bundle = Bundle(size).apply {
    forEach { (key, value) ->
        putSerializable(key, value)
    }
}
