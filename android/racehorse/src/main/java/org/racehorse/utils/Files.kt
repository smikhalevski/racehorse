package org.racehorse.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.OutputStream
import java.net.URLConnection

/**
 * Returns a MIME type of a file from its leading bytes stored in a file (a file signature).
 */
fun File.guessMimeTypeFromContent() =
    URLConnection.guessContentTypeFromStream(BufferedInputStream(inputStream()))

/**
 * The extension with the leading dot, or an empty string if there's no extension.
 */
val File.extensionSuffix get() = extension.let { if (it.isEmpty()) it else ".$it" }

/**
 * Creates a new file in the same directory, with the same extension, and name that has a unique numeric suffix.
 */
fun File.createTempFile() = File.createTempFile(nameWithoutExtension, extensionSuffix, checkNotNull(parentFile))

/**
 * Copies a normal file contents to the output stream.
 *
 * **Note:** Output stream isn't closed after operation is completed!
 */
fun File.copyTo(outputStream: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE) =
    inputStream().use { it.copyTo(outputStream, bufferSize) }
