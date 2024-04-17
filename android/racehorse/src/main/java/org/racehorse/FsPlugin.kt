package org.racehorse

import android.net.Uri
import android.os.Environment
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.guessMimeType
import org.racehorse.webview.ShouldInterceptRequestEvent
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URLConnection
import java.nio.charset.Charset
import java.nio.file.NotDirectoryException
import java.nio.file.attribute.BasicFileAttributes
import java.util.Base64
import kotlin.io.path.readAttributes

class FsIsExistingEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val isExisting: Boolean) : ResponseEvent()
}

class FsGetAttributesEvent(val uri: Uri) : RequestEvent() {
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

class FsGetParentUriEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val parentUri: Uri?) : ResponseEvent()
}

class FsGetLocalUrlEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val localUrl: Uri) : ResponseEvent()
}

class FsGetContentUriEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val contentUri: Uri) : ResponseEvent()
}

class FsGetMimeTypeEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val mimeType: String?) : ResponseEvent()
}

class FsMkdirEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsReadDirEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val fileUris: List<Uri>) : ResponseEvent()
}

class FsReadEvent(val uri: Uri, val encoding: String?) : RequestEvent() {
    class ResultEvent(val data: String) : ResponseEvent()
}

class FsWriteEvent(val uri: Uri, val data: String, val encoding: String?, val append: Boolean) : RequestEvent()

class FsCopyEvent(val uri: Uri, val toUri: Uri) : RequestEvent()

class FsDeleteEvent(val uri: Uri) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

/**
 * File system CRUD operations.
 *
 * @param activity The activity that provides access to content resolver.
 * @param providerAuthority The authority of a [FileProvider] defined in a `<provider>` element in your app's manifest.
 * @param baseLocalUrl The base local URL from which files are served to the web view.
 */
open class FsPlugin(
    val activity: ComponentActivity,
    val providerAuthority: String? = null,
    val baseLocalUrl: String = "https://fs.racehorse.local"
) {

    private companion object {
        const val SCHEME_FILE = "file"
        const val SCHEME_CONTENT = "content"
        const val SCHEME_RACEHORSE = "racehorse"

        const val DIRECTORY_DOCUMENTS = "documents"
        const val DIRECTORY_DATA = "data"
        const val DIRECTORY_LIBRARY = "library"
        const val DIRECTORY_CACHE = "cache"
        const val DIRECTORY_EXTERNAL = "external"
        const val DIRECTORY_EXTERNAL_STORAGE = "external_storage"

        const val LOCAL_URI_PARAM = "uri"
    }

    private val baseLocalUri = Uri.parse(baseLocalUrl)

    @Subscribe
    open fun onFsIsExisting(event: FsIsExistingEvent) {
        val uri = event.uri.toSupportedUri()

        event.respond(
            FsIsExistingEvent.ResultEvent(
                when (uri.scheme) {

                    SCHEME_FILE -> uri.toFile().exists()

                    SCHEME_CONTENT -> try {
                        uri.getOutputStream().close()
                        true
                    } catch (_: FileNotFoundException) {
                        false
                    }

                    else -> throw UnsupportedUriException()
                }
            )
        )
    }

    @Subscribe
    open fun onFsGetParentUri(event: FsGetParentUriEvent) {
        val uri = event.uri.toSupportedUri()

        event.respond(
            FsGetParentUriEvent.ResultEvent(
                when (uri.scheme) {

                    SCHEME_FILE -> uri.toFile().parentFile?.toUri()

                    SCHEME_CONTENT -> null

                    else -> throw UnsupportedUriException()
                }
            )
        )
    }

    @Subscribe
    open fun onFsGetLocalUrl(event: FsGetLocalUrlEvent) {
        event.respond(
            FsGetLocalUrlEvent.ResultEvent(
                baseLocalUri.buildUpon().appendQueryParameter(LOCAL_URI_PARAM, event.uri.toString()).build()
            )
        )
    }

    @Subscribe
    open fun onFsGetContentUri(event: FsGetContentUriEvent) {
        val uri = event.uri.toSupportedUri()

        event.respond(
            FsGetContentUriEvent.ResultEvent(
                when (uri.scheme) {

                    SCHEME_FILE -> FileProvider.getUriForFile(
                        activity,
                        checkNotNull(providerAuthority) { "No provider authority" },
                        uri.toFile()
                    )

                    SCHEME_CONTENT -> uri

                    else -> throw UnsupportedUriException()
                }
            )
        )
    }

    @Subscribe
    open fun onFsGetAttributes(event: FsGetAttributesEvent) {
        val uri = event.uri.toSupportedUri()

        event.respond(
            when (uri.scheme) {

                SCHEME_FILE -> uri.toFile().toPath().readAttributes<BasicFileAttributes>().run {
                    FsGetAttributesEvent.ResultEvent(
                        lastModifiedTime = lastModifiedTime().toMillis(),
                        lastAccessTime = lastAccessTime().toMillis(),
                        creationTime = creationTime().toMillis(),
                        isFile = isRegularFile,
                        isDirectory = isDirectory,
                        isSymbolicLink = isSymbolicLink,
                        isOther = isOther,
                        size = size(),
                    )
                }

                SCHEME_CONTENT -> {
                    uri.getOutputStream().close()

                    FsGetAttributesEvent.ResultEvent(
                        lastModifiedTime = 0,
                        lastAccessTime = 0,
                        creationTime = 0,
                        isFile = false,
                        isDirectory = false,
                        isSymbolicLink = false,
                        isOther = true,
                        size = -1,
                    )
                }

                else -> throw UnsupportedUriException()
            }
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsGetMimeType(event: FsGetMimeTypeEvent) {
        val uri = event.uri.toSupportedUri()

        event.respond(
            FsGetMimeTypeEvent.ResultEvent(
                when (uri.scheme) {

                    SCHEME_FILE -> uri.toFile().guessMimeType()

                    SCHEME_CONTENT -> activity.contentResolver.getType(uri)

                    else -> throw UnsupportedUriException()
                }
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsMkdir(event: FsMkdirEvent) {
        event.respond(FsMkdirEvent.ResultEvent(event.uri.toSupportedUri().toFile().mkdirs()))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsReadDir(event: FsReadDirEvent) {
        val dir = event.uri.toSupportedUri().toFile()

        event.respond(
            FsReadDirEvent.ResultEvent(
                dir.list()?.map { File(dir, it).toUri() } ?: throw NotDirectoryException(dir.absolutePath)
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsRead(event: FsReadEvent) {
        val bytes = event.uri.toSupportedUri().getInputStream().use { it.readBytes() }

        event.respond(
            FsReadEvent.ResultEvent(
                if (event.encoding == null) {
                    String(Base64.getEncoder().encode(bytes))
                } else {
                    String(bytes, Charset.forName(event.encoding))
                }
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsWrite(event: FsWriteEvent) {
        val bytes = if (event.encoding == null) {
            Base64.getDecoder().decode(event.data)
        } else {
            event.data.toByteArray(Charset.forName(event.encoding))
        }

        event.uri.toSupportedUri().getOutputStream(event.append).use { it.write(bytes) }

        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsCopy(event: FsCopyEvent) {
        event.uri.toSupportedUri().getInputStream().use {
            event.toUri.toSupportedUri().getOutputStream().use(it::copyTo)
        }
        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsDelete(event: FsDeleteEvent) {
        val uri = event.uri.toSupportedUri()

        event.respond(
            FsDeleteEvent.ResultEvent(
                when (uri.scheme) {

                    SCHEME_FILE -> uri.toFile().deleteRecursively()

                    SCHEME_CONTENT -> activity.contentResolver.delete(uri, null, null) == 1

                    else -> throw UnsupportedUriException()
                }
            )
        )
    }

    @Subscribe
    open fun onShouldInterceptRequest(event: ShouldInterceptRequestEvent) {
        if (!event.request.url.toString().startsWith(baseLocalUrl)) {
            // Unrelated request
            return
        }

        event.response = try {
            val uri = Uri.parse(event.request.url.getQueryParameter(LOCAL_URI_PARAM)).toSupportedUri()

            when (uri.scheme) {

                SCHEME_FILE -> WebResourceResponse(
                    URLConnection.guessContentTypeFromName(uri.path),
                    null,
                    FileInputStream(uri.toFile())
                )

                SCHEME_CONTENT -> WebResourceResponse(
                    activity.contentResolver.getType(uri),
                    null,
                    uri.getInputStream()
                )

                // 404
                else -> WebResourceResponse(null, null, null)
            }
        } catch (e: Throwable) {
            WebResourceResponse(
                null,
                null,
                HttpURLConnection.HTTP_INTERNAL_ERROR,
                e.message ?: "Internal error",
                null,
                ByteArrayInputStream(e.stackTraceToString().toByteArray())
            )
        }
    }

    /**
     * Returns [Uri] with `file:` or `content:` scheme. Resolves `racehorse:` scheme as a file.
     */
    protected fun Uri.toSupportedUri() = when (scheme) {
        SCHEME_FILE, SCHEME_CONTENT -> this

        SCHEME_RACEHORSE -> {
            val baseDir = when (authority) {
                DIRECTORY_DOCUMENTS ->
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)

                DIRECTORY_DATA, DIRECTORY_LIBRARY -> activity.filesDir
                DIRECTORY_CACHE -> activity.cacheDir
                DIRECTORY_EXTERNAL -> activity.getExternalFilesDir(null)
                DIRECTORY_EXTERNAL_STORAGE -> Environment.getExternalStorageDirectory()
                else -> null
            }

            requireNotNull(baseDir) { "Unrecognized directory" }

            File(baseDir, pathSegments.joinToString(File.separator)).toUri()
        }

        else -> throw UnsupportedUriException()
    }

    protected fun Uri.getInputStream() = when (scheme) {

        SCHEME_FILE -> toFile().inputStream()

        SCHEME_CONTENT -> requireNotNull(activity.contentResolver.openInputStream(this))

        else -> throw UnsupportedUriException()
    }

    protected fun Uri.getOutputStream(append: Boolean = false) = when (scheme) {

        SCHEME_FILE -> FileOutputStream(toFile(), append)

        SCHEME_CONTENT -> requireNotNull(activity.contentResolver.openOutputStream(this, if (append) "wa" else "w"))

        else -> throw UnsupportedUriException()
    }
}

private class UnsupportedUriException : Exception()
