package org.racehorse.utils

import java.security.MessageDigest

inline fun String?.ifNullOrBlank(defaultValue: () -> String): String =
    if (isNullOrBlank()) defaultValue() else this

fun String.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
