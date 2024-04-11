package org.racehorse.utils

import java.io.DataInputStream
import java.io.File
import java.io.OutputStream

/**
 * Map from a file signature to a corresponding MIME type.
 *
 * [List of file signatures](https://en.wikipedia.org/wiki/List_of_file_signatures)
 * [Binary signatures](https://www.den4b.com/wiki/ReNamer%3aBinary_Signatures)
 */
val mimeTypeSignatureMap = arrayListOf(
    0xFF_D8_FF_00_00_00_00_00U to "image/jpeg",
    0x47_49_46_38_37_61_00_00U to "image/gif",
    0x47_49_46_38_39_61_00_00U to "image/gif",
    0x89_50_4E_47_0D_0A_1A_0AU to "image/png",
    0x52_49_46_46_00_00_00_00U to "image/webp",
    0x49_49_2A_00_00_00_00_00U to "image/tiff",
    0x4D_4D_00_2A_00_00_00_00U to "image/tiff",
    0x66_74_79_70_69_73_6F_6DU to "video/mp4",
    0x66_74_79_70_4D_53_4E_56U to "video/mp4",
    0x00_00_00_18_66_74_79_70U to "video/mp4",
    0x1A_45_DF_A3_00_00_00_00U to "video/webm",
)

/**
 * Returns a MIME type of a file from its leading bytes stored in a file (a file signature).
 */
fun File.guessMimeTypeFromContent(): String? {
    val signature = DataInputStream(inputStream()).use(DataInputStream::readLong).toULong()

    return mimeTypeSignatureMap.find { (mask) -> signature and mask == mask }?.second
}

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
