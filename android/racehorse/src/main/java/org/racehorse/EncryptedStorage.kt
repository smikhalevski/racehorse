package org.racehorse

import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.BadPaddingException
import javax.crypto.Cipher

/**
 * @param iv The initialization vector to init cipher.
 * @param encryptedValue The encrypted value stored in the record.
 */
class EncryptedRecord(val iv: ByteArray, val encryptedValue: ByteArray)

/**
 * File-based encrypted storage.
 */
open class EncryptedStorage(private val storageDir: File) {

    private companion object {
        const val HASH_ALGORITHM = "SHA-512"
        const val HASH_SIZE = 64 // bytes

        /**
         * The size in bytes of the IV array.
         */
        const val IV_SIZE_SIZE = 4 // bytes
    }

    /**
     * Sets a value to a storage.
     *
     * @param cipher The cipher that is used to encode the value.
     * @param key The key under which the value is stored.
     * @param value The byte array the holds the data.
     * @return `true` if the key was successfully persisted, or `false` otherwise.
     */
    open fun set(cipher: Cipher, key: String, value: ByteArray): Boolean {
        val valueHash = MessageDigest.getInstance(HASH_ALGORITHM).digest(value)

        val ivSize = ByteBuffer.allocate(IV_SIZE_SIZE).putInt(cipher.iv.size).array()

        val fileBytes = ivSize + cipher.iv + cipher.doFinal(valueHash + value)

        return try {
            getFile(key).writeBytes(fileBytes)
            true
        } catch (_: IOException) {
            false
        }
    }

    /**
     * Retrieves the encrypted record from the storage.
     *
     * The first element holds the initialization vector.
     * The second element holds the encrypted bytes. Use [decrypt] to read the value from the encrypted bytes.
     */
    open fun getRecord(key: String): EncryptedRecord? {
        val file = getFile(key)

        if (!file.exists()) {
            return null
        }

        val fileBytes = file.readBytes()
        val ivSize = ByteBuffer.wrap(fileBytes.copyOf(IV_SIZE_SIZE)).int

        return EncryptedRecord(
            iv = fileBytes.copyOfRange(IV_SIZE_SIZE, IV_SIZE_SIZE + ivSize),
            encryptedValue = fileBytes.copyOfRange(IV_SIZE_SIZE + ivSize, fileBytes.size)
        )
    }

    /**
     * Returns `true` if there's a value associated with the key in the storage, or `false` otherwise.
     */
    open fun has(key: String) = getFile(key).exists()

    /**
     * Deletes the value associated with the key from the storage.
     */
    open fun delete(key: String) = getFile(key).delete()

    /**
     * Decrypts the encrypted value that was retrieved via [getRecord].
     *
     * Returns the decrypted value or `null` if decryption failed because of the invalid decryption key.
     */
    open fun decrypt(cipher: Cipher, encryptedValue: ByteArray): ByteArray? {
        return try {
            val bytes = cipher.doFinal(encryptedValue)

            bytes.copyOfRange(HASH_SIZE, bytes.size)
        } catch (_: BadPaddingException) {
            null
        }
    }

    open fun getFile(key: String): File {
        storageDir.mkdirs()

        val keyHash = MessageDigest.getInstance(HASH_ALGORITHM).digest(key.toByteArray())

        return File(storageDir, BigInteger(1, keyHash).toString(16))
    }
}
