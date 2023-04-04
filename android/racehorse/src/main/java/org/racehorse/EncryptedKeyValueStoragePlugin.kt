package org.racehorse

import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.*
import java.io.File
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

open class EncryptedKeyValueStoragePlugin(private val storageDir: File) : Plugin(), EventBusCapability {

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

        val spec = PBEKeySpec(password.toCharArray(), context.packageName.toByteArray(), 2048, 256)

        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun getFile(key: String): File {
        require(key.all(Char::isJavaIdentifierPart)) { "Expected key to be an identifier" }

        storageDir.mkdirs()

        return File(storageDir, "$key.ekv")
    }

    @Subscribe
    fun onSetEncryptedValueRequestEvent(event: SetEncryptedValueRequestEvent) {
        val cipher = getCipher()

        cipher.init(Cipher.ENCRYPT_MODE, getSecret(event.password))

        val bytes = cipher.iv + cipher.doFinal(event.value.toByteArray())

        getFile(event.key).writeBytes(bytes)

        postToChain(event, VoidResponseEvent())
    }

    @Subscribe
    fun onGetEncryptedValueRequestEvent(event: GetEncryptedValueRequestEvent) {
        val file = getFile(event.key);

        val value = if (file.exists()) {
            val cipher = getCipher()

            val bytes = file.readBytes()
            val iv = IvParameterSpec(bytes.copyOfRange(0, 16))

            cipher.init(Cipher.DECRYPT_MODE, getSecret(event.password), iv)

            try {
                String(cipher.doFinal(bytes.copyOfRange(16, bytes.size)))
            } catch (_: BadPaddingException) {
                throw IllegalArgumentException("Cannot decrypt a value")
            }
        } else null

        postToChain(event, GetEncryptedValueResponseEvent(value))
    }

    @Subscribe
    fun onHasEncryptedValueRequestEvent(event: HasEncryptedValueRequestEvent) {
        postToChain(event, HasEncryptedValueResponseEvent(getFile(event.key).exists()))
    }

    @Subscribe
    fun onDeleteEncryptedValueRequestEvent(event: DeleteEncryptedValueRequestEvent) {
        getFile(event.key).delete()

        postToChain(event, VoidResponseEvent())
    }
}
