package org.racehorse.permissions.events

class RequestPermissionsResultEvent(
    val requestCode: Int,
    val permissions: Array<out String>,
    val grantResults: IntArray
)
