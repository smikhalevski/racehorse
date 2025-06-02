package org.racehorse.utils

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import java.io.Serializable

fun Bundle.toMap(): Map<String, Any?> = buildMap {
    for (key in keySet()) {
        @Suppress("DEPRECATION")
        put(key, this@toMap.get(key))
    }
}

fun Map<String, *>.toBundle(): Bundle = Bundle(size).apply {
    forEach { (key, value) ->
        @Suppress("UNCHECKED_CAST")
        when (value) {
            null -> putSerializable(key, null)

            is Boolean -> putBoolean(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Double -> putDouble(key, value)
            is String -> putString(key, value)
            is BooleanArray -> putBooleanArray(key, value)
            is Byte -> putByte(key, value)
            is Char -> putChar(key, value)
            is CharSequence -> putCharSequence(key, value)
            is Short -> putShort(key, value)
            is Float -> putFloat(key, value)
            is IntArray -> putIntArray(key, value)
            is LongArray -> putLongArray(key, value)
            is DoubleArray -> putDoubleArray(key, value)
            is ByteArray -> putByteArray(key, value)
            is ShortArray -> putShortArray(key, value)
            is CharArray -> putCharArray(key, value)
            is FloatArray -> putFloatArray(key, value)
            is Array<*> -> putParcelableArray(key, value as Array<Parcelable>)
            is ArrayList<*> -> putParcelableArrayList(key, value as ArrayList<Parcelable>)
            is Bundle -> putBundle(key, value)
            is Parcelable -> putParcelable(key, value)
            is Size -> putSize(key, value)
            is Serializable -> putSerializable(key, value)
            is Binder -> putBinder(key, value)

            else -> throw IllegalArgumentException("Illegal bundle value: ${value::class.java.name}")
        }
    }
}
