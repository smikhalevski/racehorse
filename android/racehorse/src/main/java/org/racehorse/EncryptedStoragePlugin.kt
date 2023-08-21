package org.racehorse

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
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

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onSetEncryptedValue(event: SetEncryptedValueEvent) {
        val valueBytes = event.value.toByteArray()

        val cipher = getCipher()
        val digest = MessageDigest.getInstance("SHA-512")

        cipher.init(Cipher.ENCRYPT_MODE, getSecret(event.password))

        getFile(event.key).writeBytes(cipher.iv + cipher.doFinal(digest.digest(valueBytes) + valueBytes))

        event.respond(VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onGetEncryptedValue(event: GetEncryptedValueEvent) {
        val file = getFile(event.key)

        val bytes = if (file.exists()) {
            val fileBytes = file.readBytes()

            val cipher = getCipher()

            cipher.init(Cipher.DECRYPT_MODE, getSecret(event.password), IvParameterSpec(fileBytes.copyOf(16)))

            try {
                cipher.doFinal(fileBytes.copyOfRange(16, fileBytes.size))
            } catch (_: BadPaddingException) {
                null
            }
        } else null

        val value = bytes?.run { String(copyOfRange(64, size)) }

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
        return Cipher.getInstance("AES/CBC/PKCS5Padding")
    }

    /**
     * Returns the secret for a password.
     */
    protected open fun getSecret(password: String): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, iterationCount, 256)

        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    /**
     * Returns the file descriptor for a given key.
     */
    private fun getFile(key: String): File {
        storageDir.mkdirs()

        return File(storageDir, BigInteger(1, MessageDigest.getInstance("MD5").digest(key.toByteArray())).toString(16))
    }
}
