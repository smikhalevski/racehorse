package org.racehorse

import android.net.Uri
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.core.net.toFile
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.nio.charset.Charset
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.readAttributes

class FsMkdirEvent(val path: String, val directory: String?) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsReadDirEvent(val path: String, val directory: String?, val glob: String = "*") : RequestEvent() {
    class ResultEvent(val files: List<String>) : ResponseEvent()
}

class FsReadTextEvent(val path: String, val directory: String?, val encoding: String? = "utf-8") : RequestEvent() {
    class ResultEvent(val text: String) : ResponseEvent()
}

class FsReadBytesEvent(val path: String, val directory: String?) : RequestEvent() {
    class ResultEvent(val buffer: ByteArray) : ResponseEvent()
}

class FsAppendTextEvent(val path: String, val directory: String?, val text: String, val encoding: String? = "utf-8") :
    RequestEvent()

class FsAppendBytesEvent(val path: String, val directory: String?, val buffer: ByteArray) : RequestEvent()

class FsWriteTextEvent(val path: String, val directory: String?, val text: String, val encoding: String? = "utf-8") :
    RequestEvent()

class FsWriteBytesEvent(val path: String, val directory: String?, val buffer: ByteArray) : RequestEvent()

class FsCopyEvent(
    val sourcePath: String,
    val sourceDirectory: String?,
    val targetPath: String,
    val targetDirectory: String?,
    val overwrite: Boolean = false
) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsMoveEvent(
    val sourcePath: String,
    val sourceDirectory: String?,
    val targetPath: String,
    val targetDirectory: String?
) : RequestEvent()

class FsDeleteEvent(val path: String, val directory: String?) : RequestEvent() {
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

class FsExistsEvent(val path: String, val directory: String?) : RequestEvent() {
    class ResultEvent(val isExisting: Boolean) : ResponseEvent()
}

class FsStatEvent(val path: String, val directory: String?) : RequestEvent() {
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

open class FsPlugin(val activity: ComponentActivity) {

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsMkdir(event: FsMkdirEvent) {
        event.respond(FsMkdirEvent.ResultEvent(getFile(event.path, event.directory).mkdirs()))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsReadDir(event: FsReadDirEvent) {
        event.respond(FsReadDirEvent.ResultEvent(
            getFile(event.path, event.directory).toPath().listDirectoryEntries(event.glob)
                .map { it.fileName.toString() }
        ))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsReadText(event: FsReadTextEvent) {
        event.respond(
            FsReadTextEvent.ResultEvent(
                getFile(event.path, event.directory).readText(Charset.forName(event.encoding))
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsReadBytes(event: FsReadBytesEvent) {
        event.respond(FsReadBytesEvent.ResultEvent(getFile(event.path, event.directory).readBytes()))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsAppendText(event: FsAppendTextEvent) {
        getFile(event.path, event.directory).appendText(event.text, Charset.forName(event.encoding))
        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsAppendBytes(event: FsAppendBytesEvent) {
        getFile(event.path, event.directory).appendBytes(event.buffer)
        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsWriteText(event: FsWriteTextEvent) {
        getFile(event.path, event.directory).writeText(event.text, Charset.forName(event.encoding))
        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsWriteBytes(event: FsWriteBytesEvent) {
        getFile(event.path, event.directory).writeBytes(event.buffer)
        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsCopy(event: FsCopyEvent) {
        event.respond(
            FsCopyEvent.ResultEvent(
                getFile(event.sourcePath, event.sourceDirectory)
                    .copyRecursively(getFile(event.targetPath, event.targetDirectory), event.overwrite)
            )
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsMove(event: FsMoveEvent) {
        getFile(event.sourcePath, event.sourceDirectory).toPath()
            .moveTo(getFile(event.targetPath, event.targetDirectory).toPath())

        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsDelete(event: FsDeleteEvent) {
        event.respond(FsCopyEvent.ResultEvent(getFile(event.path, event.directory).deleteRecursively()))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsExists(event: FsExistsEvent) {
        event.respond(FsExistsEvent.ResultEvent(getFile(event.path, event.directory).exists()))
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onFsStat(event: FsStatEvent) {
        getFile(event.path, event.directory).toPath().readAttributes<BasicFileAttributes>().run {
            event.respond(
                FsStatEvent.ResultEvent(
                    lastModifiedTime = lastModifiedTime().toMillis(),
                    lastAccessTime = lastAccessTime().toMillis(),
                    creationTime = creationTime().toMillis(),
                    isFile = isRegularFile,
                    isDirectory = isDirectory,
                    isSymbolicLink = isSymbolicLink,
                    isOther = isOther,
                    size = size(),
                )
            )
        }
    }

    private fun getFile(path: String, directory: String?): File {
        val dir = getDirectory(directory) ?: return Uri.parse(path).toFile()

        dir.mkdirs()

        return File(dir, path)
    }

    fun getDirectory(directory: String?): File? {
        return when (directory) {
            "DOCUMENTS" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            "DATA", "LIBRARY" -> activity.filesDir
            "CACHE" -> activity.cacheDir
            "EXTERNAL" -> activity.getExternalFilesDir(null)
            "EXTERNAL_STORAGE" -> Environment.getExternalStorageDirectory()
            else -> null
        }
    }
}
