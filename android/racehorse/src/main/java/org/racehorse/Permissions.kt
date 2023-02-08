package org.racehorse

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
