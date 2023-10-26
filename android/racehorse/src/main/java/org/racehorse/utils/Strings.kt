package org.racehorse.utils

inline fun String?.ifNullOrBlank(defaultValue: () -> String): String =
    if (isNullOrBlank()) defaultValue() else this
