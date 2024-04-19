package org.racehorse.utils

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

fun <T> ContentResolver.queryAll(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    callback: Cursor.() -> T
) = checkNotNull(
    query(
        uri,
        projection,
        selection,
        selectionArgs,
        sortOrder,
    )
) { "Content resolver crashed" }.use(callback)
