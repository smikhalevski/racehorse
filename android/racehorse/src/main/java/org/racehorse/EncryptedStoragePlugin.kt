package org.racehorse

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
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
class SetEncryptedValueEvent(val key: String, val value: String, val password: String) : RequestEvent() {

    /**
     * @param isSuccessful `true` if the value was written to the storage, or `false` otherwise.
     */
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

/**
 * Retrieves an encrypted value associated with the key.
 */
class GetEncryptedValueEvent(val key: String, val password: String) : RequestEvent() {

    /**
     * @param value The deciphered value or `null` if key wasn't found or if password is incorrect.
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
 * @param keySize The size in bits of the secret key that is derived from the password.
 */
open class EncryptedStoragePlugin(
    private val storageDir: File,
    private val salt: ByteArray,
    private val iterationCount: Int = 10_000,
    private val keySize: Int = 256
) {

    companion object {
        private const val ENCRYPTION_ALGORITHM = "AES"
        private const val ENCRYPTION_BLOCK_MODE = "CBC"
        private const val ENCRYPTION_PADDING = "PKCS5Padding"
        private const val SECRET_KEY_ALGORITHM = "PBKDF2withHmacSHA256"
    }

    private val encryptedStorage = EncryptedStorage(storageDir)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onSetEncryptedValue(event: SetEncryptedValueEvent) {
        val cipher = createCipher()
        cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(event.password))

        val result = encryptedStorage.set(cipher, event.key, event.value.toByteArray(Charsets.UTF_8))
        event.respond(SetEncryptedValueEvent.ResultEvent(result))
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetEncryptedValue(event: GetEncryptedValueEvent) {
        val record = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetEncryptedValueEvent.ResultEvent(null))

        val cipher = createCipher()
        cipher.init(Cipher.DECRYPT_MODE, createSecretKey(event.password), IvParameterSpec(record.iv))

        val value = encryptedStorage.decrypt(cipher, record.encryptedBytes)?.toString(Charsets.UTF_8)
        event.respond(GetEncryptedValueEvent.ResultEvent(value))
    }

    @Subscribe
    open fun onHasEncryptedValue(event: HasEncryptedValueEvent) {
        event.respond(HasEncryptedValueEvent.ResultEvent(encryptedStorage.has(event.key)))
    }

    @Subscribe
    open fun onDeleteEncryptedValue(event: DeleteEncryptedValueEvent) {
        event.respond(DeleteEncryptedValueEvent.ResultEvent(encryptedStorage.delete(event.key)))
    }

    /**
     * Returns the [Cipher] instance that is used for encoding/decoding.
     */
    protected open fun createCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    /**
     * Returns the secret key for a password.
     */
    protected open fun createSecretKey(password: String): SecretKey {
        val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterationCount, keySize)

        return SecretKeySpec(factory.generateSecret(keySpec).encoded, ENCRYPTION_ALGORITHM)
    }
}
