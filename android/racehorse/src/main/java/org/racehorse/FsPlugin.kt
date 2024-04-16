package org.racehorse

import android.net.Uri
import android.os.Environment
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import androidx.core.net.toFile
import androidx.core.net.toUri
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.guessMimeTypeFromContent
import org.racehorse.utils.queryContent
import org.racehorse.webview.ShouldInterceptRequestEvent
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.file.attribute.BasicFileAttributes
import java.util.Base64
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.readAttributes

class FsIsExistingEvent(val uri: String) : RequestEvent() {
    class ResultEvent(val isExisting: Boolean) : ResponseEvent()
}

class FsGetStatEvent(val uri: String) : RequestEvent() {
    class ResultEvent(
        val lastModifiedTime: Long,
        val lastAccessTime: Long,
        val creationTime: Long,
        val isFile: Boolean,
        val isDirectory: Boolean,
        val isSymbolicLink: Boolean,
        val isOther: Boolean,
        val size: Long,
    ) : ResponseEvent()
}

class FsGetMimeTypeEvent(val uri: String) : RequestEvent() {
    class ResultEvent(val mimeType: String?) : ResponseEvent()
}

class FsMkdirEvent(val uri: String) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsReadDirEvent(val uri: String) : RequestEvent() {
    class ResultEvent(val uris: List<String>) : ResponseEvent()
}

class FsReadEvent(val uri: String, val encoding: String?) : RequestEvent() {
    class ResultEvent(val data: String) : ResponseEvent()
}

class FsAppendEvent(val uri: String, val data: String, val encoding: String?) : RequestEvent()

class FsWriteEvent(val uri: String, val data: String, val encoding: String?) : RequestEvent()

class FsCopyEvent(val uri: String, val toUri: String, val overwrite: Boolean) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsMoveEvent(val uri: String, val toUri: String, val overwrite: Boolean) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsDeleteEvent(val uri: String) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsGetUrlBaseEvent : RequestEvent() {
    class ResultEvent(val urlBase: String) : ResponseEvent()
}

class FsResolveEvent(val uri: String, val path: String) : RequestEvent() {
    class ResultEvent(val uri: String) : ResponseEvent()
}

open class FsPlugin(val activity: ComponentActivity, val urlBase: String = "https://racehorse.local/fs/") {

    private companion object {
        const val SCHEME_FILE = "file"
        const val SCHEME_CONTENT = "content"
        const val SCHEME_RACEHORSE = "racehorse"

        const val SYSTEM_DIR_DOCUMENTS = "documents"
        const val SYSTEM_DIR_DATA = "data"
        const val SYSTEM_DIR_LIBRARY = "library"
        const val SYSTEM_DIR_CACHE = "cache"
        const val SYSTEM_DIR_EXTERNAL = "external"
        const val SYSTEM_DIR_EXTERNAL_STORAGE = "external_storage"
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsIsExisting(event: FsIsExistingEvent) {
        val uri = getCanonicalUri(event.uri)

        event.respond(FsIsExistingEvent.ResultEvent(
            when (uri.scheme) {

                SCHEME_FILE -> uri.toFile().exists()

                SCHEME_CONTENT -> activity.queryContent(uri) { moveToFirst() }

                else -> throw UnsupportedSchemeException()
            }
        ))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsGetStat(event: FsGetStatEvent) {
        event.respond(getFile(event.uri).toPath().readAttributes<BasicFileAttributes>().run {
            FsGetStatEvent.ResultEvent(
                lastModifiedTime = lastModifiedTime().toMillis(),
                lastAccessTime = lastAccessTime().toMillis(),
                creationTime = creationTime().toMillis(),
                isFile = isRegularFile,
                isDirectory = isDirectory,
                isSymbolicLink = isSymbolicLink,
                isOther = isOther,
                size = size(),
            )
        })
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsGetMimeType(event: FsGetMimeTypeEvent) {
        val uri = getCanonicalUri(event.uri)

        event.respond(
            FsGetMimeTypeEvent.ResultEvent(
                when (uri.scheme) {

                    SCHEME_FILE -> runCatching { uri.toFile().guessMimeTypeFromContent() }.getOrNull()

                    SCHEME_CONTENT -> activity.contentResolver.getType(uri)

                    else -> throw UnsupportedSchemeException()
                }
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsMkdir(event: FsMkdirEvent) {
        event.respond(FsMkdirEvent.ResultEvent(getFile(event.uri).mkdirs()))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsReadDir(event: FsReadDirEvent) {
        event.respond(
            FsReadDirEvent.ResultEvent(
                getFile(event.uri).toPath().listDirectoryEntries().map { it.fileName.toString() }
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsRead(event: FsReadEvent) {
        val uri = getCanonicalUri(event.uri)

        val bytes = when (uri.scheme) {

            SCHEME_FILE -> uri.toFile().readBytes()

            SCHEME_CONTENT -> requireNotNull(activity.contentResolver.openInputStream(uri)).use { it.readBytes() }

            else -> throw UnsupportedSchemeException()
        }

        val data = if (event.encoding == null) {
            String(Base64.getEncoder().encode(bytes))
        } else {
            String(bytes, Charset.forName(event.encoding))
        }

        event.respond(FsReadEvent.ResultEvent(data))
    }

    private fun write(uri: Uri, data: String, encoding: String?, append: Boolean) {
        val outputStream = when (uri.scheme) {

            SCHEME_FILE -> FileOutputStream(uri.toFile(), append)

            SCHEME_CONTENT -> requireNotNull(activity.contentResolver.openOutputStream(uri, if (append) "wa" else "w"))

            else -> throw UnsupportedSchemeException()
        }

        outputStream.use {
            val bytes = if (encoding == null) {
                Base64.getDecoder().decode(data)
            } else {
                data.toByteArray(Charset.forName(encoding))
            }
            it.write(bytes)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsAppend(event: FsAppendEvent) {
        write(getCanonicalUri(event.uri), event.data, event.encoding, true)
        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsWrite(event: FsWriteEvent) {
        write(getCanonicalUri(event.uri), event.data, event.encoding, false)
        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsCopy(event: FsCopyEvent) {
        event.respond(
            FsCopyEvent.ResultEvent(
                getFile(event.uri).copyRecursively(getFile(event.toUri), event.overwrite)
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsMove(event: FsMoveEvent) {
        getFile(event.uri).toPath().moveTo(getFile(event.toUri).toPath(), event.overwrite)

        event.respond(FsCopyEvent.ResultEvent(true))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsDelete(event: FsDeleteEvent) {
        val uri = getCanonicalUri(event.uri)

        event.respond(
            FsDeleteEvent.ResultEvent(
                when (uri.scheme) {

                    SCHEME_FILE -> uri.toFile().deleteRecursively()

                    SCHEME_CONTENT -> activity.contentResolver.delete(uri, null, null) == 1

                    else -> throw UnsupportedSchemeException()
                }
            )
        )
    }

    @Subscribe
    open fun onFsGetUrlBase(event: FsGetUrlBaseEvent) {
        event.respond(FsGetUrlBaseEvent.ResultEvent(urlBase))
    }

    @Subscribe
    open fun onFsResolve(event: FsResolveEvent) {
        val baseUri = getCanonicalUri(event.uri)

        baseUri.scheme == SCHEME_FILE || throw UnsupportedSchemeException()

        val uri = Uri.parse(event.path)

        event.respond(
            FsResolveEvent.ResultEvent(
                when {
                    !uri.scheme.isNullOrBlank() -> uri.toString()

                    uri.pathSegments.isEmpty() -> baseUri.toString()

                    else -> baseUri.buildUpon().apply { uri.pathSegments.forEach(::appendPath) }.build().toString()
                }
            )
        )
    }

    @Subscribe
    open fun onShouldInterceptRequest(event: ShouldInterceptRequestEvent) {
        if (!event.request.url.toString().startsWith(urlBase)) {
            return
        }

        event.response = try {
            val uri = Uri.parse(requireNotNull(event.request.url.getQueryParameter("uri")) { "Expected resource URI" })

            when (uri.scheme) {

                SCHEME_FILE -> WebResourceResponse(
                    URLConnection.guessContentTypeFromName(uri.path),
                    null,
                    FileInputStream(uri.toFile())
                )

                SCHEME_CONTENT -> WebResourceResponse(
                    activity.contentResolver.getType(uri),
                    null,
                    activity.contentResolver.openInputStream(uri)
                )

                // 404
                else -> WebResourceResponse(null, null, null)
            }
        } catch (e: Throwable) {
            WebResourceResponse(
                null,
                null,
                500,
                e.message ?: "Cannot read from URI",
                null,
                ByteArrayInputStream(e.stackTraceToString().toByteArray())
            )
        }
    }

    protected fun getCanonicalUri(uriSource: String): Uri {
        val uri = Uri.parse(uriSource)

        return when (uri.scheme) {
            SCHEME_FILE, SCHEME_CONTENT -> uri

            SCHEME_RACEHORSE -> {
                val baseDir = when (uri.authority) {
                    SYSTEM_DIR_DOCUMENTS ->
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

                    SYSTEM_DIR_DATA, SYSTEM_DIR_LIBRARY -> activity.filesDir
                    SYSTEM_DIR_CACHE -> activity.cacheDir
                    SYSTEM_DIR_EXTERNAL -> activity.getExternalFilesDir(null)
                    SYSTEM_DIR_EXTERNAL_STORAGE -> Environment.getExternalStorageDirectory()
                    else -> null
                }

                requireNotNull(baseDir) { "Unrecognized directory" }

                File(baseDir, uri.pathSegments.joinToString(File.separator)).toUri()
            }

            else -> throw UnsupportedSchemeException()
        }
    }

    protected fun getFile(uri: String) = getCanonicalUri(uri).toFile()
}

class UnsupportedSchemeException : Exception()
