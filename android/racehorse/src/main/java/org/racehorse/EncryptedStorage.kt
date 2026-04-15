package org.racehorse

import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException

/**
 * @param salt The salt used to derive the encryption key.
 * @param iv The initialization vector to init cipher.
 * @param encryptedValue The encrypted value stored in the record.
 */
class EncryptedRecord(val salt: ByteArray, val iv: ByteArray, val encryptedValue: ByteArray)

/**
 * File-based encrypted storage.
 */
open class EncryptedStorage(private val storageDir: File) {

    companion object {
        private const val HASH_ALGORITHM = "SHA-512"
        private const val HASH_SIZE = 64

        /**
         * The size in bytes of the field that stores the IV or salt length.
         */
        private const val LENGTH_SIZE = 4

        const val MAX_IV_LENGTH = 4096
        const val MAX_SALT_LENGTH = 1024
    }

    open fun set(cipher: Cipher, key: String, value: ByteArray) = set(cipher, key, ByteArray(0), value)

        /**
     * Stores a value under the given key.
     *
     * The cipher must be initialized with a fresh, random IV before calling this method.
     *
     * The IV is taken from [cipher.iv] and stored together with the encrypted data.
     *
     * @return `true` if the value was successfully persisted, `false` otherwise.
     */
    @Synchronized
    open fun set(cipher: Cipher, key: String, salt: ByteArray, value: ByteArray): Boolean {
        val iv = cipher.iv

        require(iv.size >= 0 && iv.size <= MAX_IV_LENGTH) { "Invalid cipher IV size" }

        require(salt.size <= MAX_SALT_LENGTH) { "Invalid salt size" }

        val valueHash = MessageDigest.getInstance(HASH_ALGORITHM).digest(value)

        val encryptedValue = try {
            cipher.doFinal(valueHash + value)
        } catch (_: Exception) {
            return false
        }

        val saltLengthBytes = ByteBuffer.allocate(LENGTH_SIZE).putInt(salt.size).array()
        val ivLengthBytes = ByteBuffer.allocate(LENGTH_SIZE).putInt(iv.size).array()

        val fileBytes = saltLengthBytes + salt + ivLengthBytes + iv + encryptedValue

        val file = getFile(key)

        val tempFile = File.createTempFile(file.name, ".tmp")

        return try {
            tempFile.writeBytes(fileBytes)

            if (!tempFile.renameTo(file)) {
                file.delete()
                tempFile.renameTo(file)
            }
            true
        } catch (_: IOException) {
            false
        } finally {
            tempFile.delete()
        }
    }

    /**
     * Retrieves the encrypted record from the storage.
     *
     * The first element holds the initialization vector.
     * The second element holds the encrypted bytes. Use [decrypt] to read the value from the encrypted bytes.
     */
    @Synchronized
    open fun getRecord(key: String): EncryptedRecord? {
        val file = getFile(key)

        if (!file.exists()) {
            return null
        }

        val fileBytes = try {
            file.readBytes()
        } catch (_: IOException) {
            return null
        }

        if (fileBytes.size < LENGTH_SIZE) {
            return null
        }

        val saltLength = ByteBuffer.wrap(fileBytes.copyOf(LENGTH_SIZE)).int
        if (saltLength < 0 || saltLength > MAX_SALT_LENGTH) {
            return null
        }

        val saltStart = LENGTH_SIZE
        val saltEnd = saltStart + saltLength

        if (fileBytes.size < saltEnd + LENGTH_SIZE) {
            return null
        }

        val ivLength = ByteBuffer.wrap(fileBytes.copyOfRange(saltEnd, saltEnd + LENGTH_SIZE)).int
        if (ivLength < 0 || ivLength > MAX_IV_LENGTH) {
            return null
        }

        val ivStart = saltEnd + LENGTH_SIZE
        val ivEnd = ivStart + ivLength

        if (fileBytes.size < ivEnd) {
            return null
        }

        return EncryptedRecord(
            salt = fileBytes.copyOfRange(saltStart, saltEnd),
            iv = fileBytes.copyOfRange(ivStart, ivEnd),
            encryptedValue = fileBytes.copyOfRange(ivEnd, fileBytes.size)
        )
    }

    /**
     * Returns `true` if there is a stored record for the given key (even if the file is corrupt).
     */
    @Synchronized
    open fun has(key: String) = getFile(key).exists()

    /**
     * Deletes the record associated with the key.
     *
     * @return `true` if the record was successfully deleted, `false` on otherwise.
     */
    @Synchronized
    open fun delete(key: String) = getFile(key).delete()

    /**
     * Decrypts the encrypted value that was retrieved via [getRecord].
     *
     * **Note:** The [cipher] must be initialized with the IV from the [EncryptedRecord] before calling this method.
     *
     * @return The decrypted value, or `null` if decryption failed (wrong key, corrupted data, or hash mismatch).
     */
    @Synchronized
    open fun decrypt(cipher: Cipher, encryptedValue: ByteArray): ByteArray? {
        return try {
            val bytes = cipher.doFinal(encryptedValue)

            if (bytes.size < HASH_SIZE) {
                return null
            }

            val value = bytes.copyOfRange(HASH_SIZE, bytes.size)

            val storedHash = bytes.copyOfRange(0, HASH_SIZE)
            val actualHash = MessageDigest.getInstance(HASH_ALGORITHM).digest(value)

            if (MessageDigest.isEqual(storedHash, actualHash)) value else null
        } catch (_: IllegalBlockSizeException) {
            null
        } catch (_: BadPaddingException) {
            null
        }
    }

    /**
     * Returns the file used to store the record for the given key.
     *
     * The file name is derived from a SHA‑512 hash of the key, encoded as a fixed‑length hex string.
     */
    @Synchronized
    open fun getFile(key: String): File {
        storageDir.mkdirs()

        val keyHash = MessageDigest.getInstance(HASH_ALGORITHM).digest(key.toByteArray())

        // Fixed-length hex string
        val keyHex = keyHash.joinToString("") { "%02x".format(it) }

        return File(storageDir, keyHex)
    }
}
