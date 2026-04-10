package org.racehorse

import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.security.SecureRandom
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
@Serializable
open class SetEncryptedValueEvent(val key: String, val value: String, val password: String) : RequestEvent() {

    /**
     * @param isSuccessful `true` if the value was written to the storage, or `false` otherwise.
     */
    @Serializable
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

/**
 * Retrieves an encrypted value associated with the key.
 */
@Serializable
open class GetEncryptedValueEvent(val key: String, val password: String) : RequestEvent() {

    /**
     * @param value The deciphered value or `null` if key wasn't found or if password is incorrect.
     */
    @Serializable
    class ResultEvent(val value: String?) : ResponseEvent()
}

/**
 * Checks that the key exists in the storage.
 */
@Serializable
open class HasEncryptedValueEvent(val key: String) : RequestEvent() {

    @Serializable
    class ResultEvent(val isExisting: Boolean) : ResponseEvent()
}

/**
 * Deletes an encrypted value associated with the key.
 */
@Serializable
open class DeleteEncryptedValueEvent(val key: String) : RequestEvent() {

    @Serializable
    class ResultEvent(val isDeleted: Boolean) : ResponseEvent()
}

/**
 * An encrypted key-value file-based storage.
 *
 * @param storageDir The directory to write files to.
 * @param secretKeyIterationCount The number of iterations to generate an encryption key.
 * @param secretKeySize The size in bits of the secret key that is derived from the password.
 */
open class EncryptedStoragePlugin(
    private val storageDir: File,
    private val secretKeyIterationCount: Int = 300_000,
    private val secretKeySize: Int = 256
) {

    private companion object {
        const val ENCRYPTION_ALGORITHM = "AES"
        const val ENCRYPTION_BLOCK_MODE = "CBC"
        const val ENCRYPTION_PADDING = "PKCS5Padding"
        const val SECRET_KEY_ALGORITHM = "PBKDF2withHmacSHA256"
        const val SECRET_SALT_LENGTH = 32
    }

    private val secureRandom = SecureRandom()
    private val encryptedStorage = EncryptedStorage(storageDir)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onSetEncryptedValue(event: SetEncryptedValueEvent) {
        val salt = nextSalt()

        val cipher = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, createSecretKey(event.password, salt))

        val result = encryptedStorage.set(cipher, event.key, salt, event.value.toByteArray(Charsets.UTF_8))
        event.respond(SetEncryptedValueEvent.ResultEvent(result))
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetEncryptedValue(event: GetEncryptedValueEvent) {
        val record = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetEncryptedValueEvent.ResultEvent(null))

        val cipher = getCipher()
        cipher.init(Cipher.DECRYPT_MODE, createSecretKey(event.password, record.salt), IvParameterSpec(record.iv))

        val value = encryptedStorage.decrypt(cipher, record.encryptedValue)?.toString(Charsets.UTF_8)
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
    protected open fun getCipher() =
        Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")

    protected open fun nextSalt() = ByteArray(SECRET_SALT_LENGTH).apply(secureRandom::nextBytes)

    /**
     * Returns the secret key for a password.
     */
    protected open fun createSecretKey(password: String, salt: ByteArray): SecretKey {
        val factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGORITHM)
        val keySpec = PBEKeySpec(password.toCharArray(), salt, secretKeyIterationCount, secretKeySize)

        return try {
            SecretKeySpec(factory.generateSecret(keySpec).encoded, ENCRYPTION_ALGORITHM)
        } finally {
            keySpec.clearPassword()
        }
    }
}
