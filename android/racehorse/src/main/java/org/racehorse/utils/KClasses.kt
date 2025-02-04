package org.racehorse.utils

import kotlin.reflect.KClass

private val classCache = HashMap<String, KClass<*>>()

/**
 * Loads a [KClass] by its name or returns a cached class.
 */
fun loadClass(className: String): KClass<*> = classCache.getOrPut(className) { Class.forName(className).kotlin }
