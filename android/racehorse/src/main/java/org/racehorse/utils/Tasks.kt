package org.racehorse.utils

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

/**
 * Returns the result or throws an [ApiException].
 */
val <T> Task<T>.apiResult: T get() = getResult(ApiException::class.java)
