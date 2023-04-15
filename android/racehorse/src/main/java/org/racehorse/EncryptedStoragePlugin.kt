package org.racehorse

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.postToChain
import java.io.File
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
class SetEncryptedValueRequestEvent(val key: String, val value: String, val password: String) : RequestEvent()

/**
 * Retrieves an encrypted value associated with the key.
 */
class GetEncryptedValueRequestEvent(val key: String, val password: String) : RequestEvent()

/**
 * The deciphered value or `null` if key wasn't found.
 */
class GetEncryptedValueResponseEvent(val value: String?) : ResponseEvent()

/**
 * Checks that the key exists in the storage.
 */
class HasEncryptedValueRequestEvent(val key: String) : RequestEvent()

class HasEncryptedValueResponseEvent(val exists: Boolean) : ResponseEvent()

/**
 * Deletes an encrypted value associated with the key.
 */
class DeleteEncryptedValueRequestEvent(val key: String) : RequestEvent()

/**
 * An encrypted key-value file-based storage.
 *
 * @param storageDir The directory to write files to.
 * @param salt The salt required to generate the encryption key.
 * @param iterationCount The number of iterations to generate an encryption key.
 * @param eventBus The event bus to which events are posted.
 */
open class EncryptedStoragePlugin(
    private val storageDir: File,
    private val salt: ByteArray,
    private val iterationCount: Int = 10_000,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onSetEncryptedValue(event: SetEncryptedValueRequestEvent) {
        val valueBytes = event.value.toByteArray()

        val cipher = getCipher()
        val digest = MessageDigest.getInstance("SHA-512")

        cipher.init(Cipher.ENCRYPT_MODE, getSecret(event.password))

        getFile(event.key).writeBytes(cipher.iv + cipher.doFinal(digest.digest(valueBytes) + valueBytes))

        eventBus.postToChain(event, VoidEvent())
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    open fun onGetEncryptedValue(event: GetEncryptedValueRequestEvent) {
        val file = getFile(event.key)

        val value = if (file.exists()) {
            val fileBytes = file.readBytes()

            val cipher = getCipher()

            cipher.init(Cipher.DECRYPT_MODE, getSecret(event.password), IvParameterSpec(fileBytes.copyOf(16)))

            val bytes = try {
                cipher.doFinal(fileBytes.copyOfRange(16, fileBytes.size))
            } catch (_: BadPaddingException) {
                throw IllegalArgumentException("Cannot decrypt a value")
            }

            String(bytes.copyOfRange(64, bytes.size))
        } else null

        eventBus.postToChain(event, GetEncryptedValueResponseEvent(value))
    }

    @Subscribe
    open fun onHasEncryptedValue(event: HasEncryptedValueRequestEvent) {
        eventBus.postToChain(event, HasEncryptedValueResponseEvent(getFile(event.key).exists()))
    }

    @Subscribe
    open fun onDeleteEncryptedValue(event: DeleteEncryptedValueRequestEvent) {
        getFile(event.key).delete()

        eventBus.postToChain(event, VoidEvent())
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

    private fun getFile(key: String): File {
        require(key.all(Char::isJavaIdentifierPart)) { "Expected key to be an identifier" }

        storageDir.mkdirs()

        return File(storageDir, "$key.ekv")
    }
}
