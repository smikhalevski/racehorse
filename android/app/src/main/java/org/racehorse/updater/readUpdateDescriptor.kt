package org.racehorse.updater

import org.json.JSONObject
import java.net.URL

data class UpdateDescriptor(val version: String, val url: String)

fun readUpdateDescriptor(url: String): UpdateDescriptor {
    val json = JSONObject(URL(url).readText())

    return UpdateDescriptor(json.getString("version"), json.getString("url"))
}
