package com.example.myapplication

import com.google.gson.Gson
import okhttp3.*

enum class UpdateMode1 { LAZY, MANDATORY }

class UpdateInfo {
    lateinit var archiveUrl: String
    lateinit var updateMode: UpdateMode

    var binaryVersion = 0
    var assetsVersion = 0
}

private val httpClient = OkHttpClient()
private val gson = Gson()


fun Request.fetch() {}












fun <T> fetchJson(url: String, bodyClass: Class<T>): T = fetchJson(Request.Builder().url(url).build(), bodyClass)

fun <T> fetchJson(request: Request, bodyClass: Class<T>): T {
    val url = request.url()
    val username = url.username()
    val password = url.password()
    var req = request

    if (username.isNotEmpty() || password.isNotEmpty()) {
        req = request.newBuilder().addHeader("authorization", Credentials.basic(username, password)).build()
    }

    return gson.fromJson(httpClient.newCall(req).execute().body()?.string(), bodyClass)
}



// Download update info
// Compare with lo


suspend fun fetchUpdateInfo(url: String) {
    val request = Request.Builder()
        .url("http://publicobject.com/helloworld.txt")
        .build()

//    httpClient.newCall(request).
}
