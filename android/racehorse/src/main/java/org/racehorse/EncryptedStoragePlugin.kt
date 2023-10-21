package org.racehorse

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Associates a value with a key in an encrypted storage.
 *
 * @param key A key to set. Must be a valid file name.
 * @param value A value to write to the file.
 * @param password The password that is used to cipher the file contents.
 */
class SetEncryptedValueEvent(val key: String, val value: String, val password: String) : RequestEvent()

/**
 * Retrieves an encrypted value associated with the key.
 */
class GetEncryptedValueEvent(val key: String, val password: String) : RequestEvent() {

    /**
     * The deciphered value or `null` if key wasn't found or if password is incorrect.
     */
    class ResultEvent(val value: String?) : ResponseEvent()
}

/**
 * Checks that the key exists in the storage.
 */
class HasEncryptedValueEvent(val key: String) : RequestEvent() {
    class ResultEvent(val exists: Boolean) : ResponseEvent()
}

/**
 * Deletes an encrypted value associated with the key.
 */
class DeleteEncryptedValueEvent(val key: String) : RequestEvent() {
    class ResultEvent(val deleted: Boolean) : ResponseEvent()
}

/**
 * An encrypted key-value file-based storage.
 *
 * @param storageDir The directory to write files to.
 * @param salt The salt required to generate the encryption key.
 * @param iterationCount The number of iterations to generate an encryption key.
 */
open class EncryptedStoragePlugin(
    private val storageDir: File,
    private val salt: ByteArray,
    private val iterationCount: Int = 10_000
) {

    companion object {
        private const val CIPHER_ALGORITHM = "AES"
        private const val CIPHER_BLOCK_MODE = "CBC"
        private const val CIPHER_PADDING = "PKCS5Padding"
        private const val SECRET_KEY_ALGORITHM = "PBKDF2withHmacSHA256"
        private const val SECRET_KEY_LENGTH = 256
        private const val KEY_HASH_ALGORITHM = "MD5"
        private const val VALUE_HASH_ALGORITHM = "SHA-512"
        private const val VALUE_HASH_LENGTH = 64 // bytes
        private const val IV_LENGTH = 16 // bytes
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onSetEncryptedValue(event: SetEncryptedValueEvent) {
        val valueBytes = event.value.toByteArray()

        val cipher = getCipher()
        val digest = MessageDigest.getInstance(VALUE_HASH_ALGORITHM)

        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(event.password))

        getFile(event.key).writeBytes(cipher.iv + cipher.doFinal(digest.digest(valueBytes) + valueBytes))

        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetEncryptedValue(event: GetEncryptedValueEvent) {
        val file = getFile(event.key)

        val bytes = if (file.exists()) {
            val fileBytes = file.readBytes()

            val cipher = getCipher()

            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(event.password), IvParameterSpec(fileBytes.copyOf(IV_LENGTH)))

            try {
                cipher.doFinal(fileBytes.copyOfRange(IV_LENGTH, fileBytes.size))
            } catch (_: BadPaddingException) {
                null
            }
        } else null

        val value = bytes?.run { String(copyOfRange(VALUE_HASH_LENGTH, size)) }

        event.respond(GetEncryptedValueEvent.ResultEvent(value))
    }

    @Subscribe
    open fun onHasEncryptedValue(event: HasEncryptedValueEvent) {
        event.respond(HasEncryptedValueEvent.ResultEvent(getFile(event.key).exists()))
    }

    @Subscribe
    open fun onDeleteEncryptedValue(event: DeleteEncryptedValueEvent) {
        event.respond(DeleteEncryptedValueEvent.ResultEvent(getFile(event.key).delete()))
    }

    /**
     * Returns the [Cipher] instance that is used for encoding/decoding.
     */
    protected open fun getCipher(): Cipher {
        return Cipher.getInstance("$CIPHER_ALGORITHM/$CIPHER_BLOCK_MODE/$CIPHER_PADDING")
    }

    /**
     * Returns the secret key for a password.
     */
    protected open fun getSecretKey(password: String): SecretKey {
        val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
        val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, SECRET_KEY_LENGTH)

        return SecretKeySpec(factory.generateSecret(spec).encoded, CIPHER_ALGORITHM)
    }

    /**
     * Returns the file descriptor for a given key.
     */
    private fun getFile(key: String): File {
        storageDir.mkdirs()

        val keyHash = MessageDigest.getInstance(KEY_HASH_ALGORITHM).digest(key.toByteArray())

        return File(storageDir, BigInteger(1, keyHash).toString(16))
    }
}
