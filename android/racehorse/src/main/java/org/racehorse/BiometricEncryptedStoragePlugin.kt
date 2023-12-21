package org.racehorse

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.checkForeground
import org.racehorse.utils.ifNullOrBlank
import java.io.File
import java.io.Serializable
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class BiometricConfig(
    val title: String?,
    val subtitle: String?,
    val description: String?,
    val negativeButtonText: String?,
    val authenticators: Array<BiometricAuthenticator>?
) : Serializable

/**
 * Associates a value with a key in a biometric encrypted storage.
 *
 * @param key A key to set. Must be a valid file name.
 * @param value A value to write to the file.
 * @param config Options of the prompt shown to the user.
 */
class SetBiometricEncryptedValueEvent(
    val key: String,
    val value: String,
    val config: BiometricConfig?
) : RequestEvent() {

    /**
     * @param isSuccessful `true` if the value was written to the storage, or `false` otherwise.
     */
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

/**
 * Retrieves a biometric encrypted value associated with the key.
 */
class GetBiometricEncryptedValueEvent(val key: String, val config: BiometricConfig?) : RequestEvent() {

    /**
     * @param value The deciphered value or `null` if key wasn't found or auth has failed.
     */
    class ResultEvent(val value: String?) : ResponseEvent()
}

/**
 * Checks that the key exists in the storage.
 */
class HasBiometricEncryptedValueEvent(val key: String) : RequestEvent() {
    class ResultEvent(val exists: Boolean) : ResponseEvent()
}

/**
 * Deletes a value associated with the key.
 */
class DeleteBiometricEncryptedValueEvent(val key: String) : RequestEvent() {
    class ResultEvent(val deleted: Boolean) : ResponseEvent()
}

/**
 * A biometric encrypted key-value file-based storage.
 *
 * @param storageDir The directory to write files to.
 */
open class BiometricEncryptedStoragePlugin(private val activity: FragmentActivity, private val storageDir: File) {

    companion object {
        private const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val SECRET_KEYSTORE_TYPE = "AndroidKeyStore"
        private const val SECRET_KEY_SIZE = 256
    }

    private val secretKeyStore by lazy { KeyStore.getInstance(SECRET_KEYSTORE_TYPE).apply { load(null) } }

    private val biometricManager by lazy { BiometricManager.from(activity) }

    private val encryptedStorage = EncryptedStorage(storageDir)

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onSetBiometricEncryptedValue(event: SetBiometricEncryptedValueEvent) {
        activity.checkForeground()

        val value = event.value.toByteArray(Charsets.UTF_8)

        val cipher = createCipher()
        try {
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(event.key))
        } catch (_: KeyPermanentlyInvalidatedException) {
            // The previous value cannot ever be decrypted, recreate the secret key
            deleteKey(event.key)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(event.key))
        }

        authenticate(event.config, cipher) { isSuccess ->
            event.respond {
                SetBiometricEncryptedValueEvent.ResultEvent(
                    if (isSuccess) {
                        try {
                            encryptedStorage.set(cipher, event.key, value)
                        } catch (e: IllegalBlockSizeException) {
                            // https://stackoverflow.com/questions/36043912
                            deleteKey(event.key)
                            throw e
                        }
                    } else false
                )
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        activity.checkForeground()

        val record = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetEncryptedValueEvent.ResultEvent(null))

        val cipher = createCipher()
        try {
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(event.key), IvParameterSpec(record.iv))
        } catch (_: KeyPermanentlyInvalidatedException) {
            // The value cannot ever be decrypted
            deleteKey(event.key)
            event.respond(GetEncryptedValueEvent.ResultEvent(null))
            return
        }

        authenticate(event.config, cipher) { isSuccess ->
            event.respond {
                GetEncryptedValueEvent.ResultEvent(
                    if (isSuccess) {
                        try {
                            encryptedStorage.decrypt(cipher, record.encryptedBytes)?.toString(Charsets.UTF_8)
                        } catch (e: IllegalBlockSizeException) {
                            // https://stackoverflow.com/questions/36043912
                            deleteKey(event.key)
                            throw e
                        }
                    } else null
                )
            }
        }
    }

    @Subscribe
    open fun onHasBiometricEncryptedValue(event: HasBiometricEncryptedValueEvent) {
        event.respond(HasBiometricEncryptedValueEvent.ResultEvent(encryptedStorage.has(event.key)))
    }

    @Subscribe
    open fun onDeleteBiometricEncryptedValue(event: DeleteBiometricEncryptedValueEvent) {
        event.respond(DeleteBiometricEncryptedValueEvent.ResultEvent(deleteKey(event.key)))
    }

    protected open fun authenticate(config: BiometricConfig?, cipher: Cipher, callback: (isSuccess: Boolean) -> Unit) {
        val authenticators = BiometricAuthenticator.from(config?.authenticators)

        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            callback(false)
            return
        }

        val authCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) = callback(false)

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = callback(true)

            override fun onAuthenticationFailed() {}
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config?.title.ifNullOrBlank { "Authentication required" })
            .setSubtitle(config?.subtitle)
            .setDescription(config?.description)
            .setAllowedAuthenticators(authenticators)

        if (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
            promptInfoBuilder.setNegativeButtonText(config?.negativeButtonText.ifNullOrBlank { "Discard" })
        }

        try {
            BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), authCallback)
                .authenticate(promptInfoBuilder.build(), BiometricPrompt.CryptoObject(cipher))
        } catch (e: Throwable) {
            e.printStackTrace()
            callback(false)
        }
    }

    protected open fun createCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    /**
     * Returns the secret key that is used to encrypt/decrypt the value stored in biometric-encrypted storage.
     *
     * @param key The biometric-encrypted storage key.
     */
    protected open fun getOrCreateSecretKey(key: String): SecretKey {
        secretKeyStore.getKey(key, null)?.let { return it as SecretKey }

        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder(key, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setKeySize(SECRET_KEY_SIZE)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)
                .build()

        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, SECRET_KEYSTORE_TYPE)
        keyGenerator.init(keyGenParameterSpec)

        return keyGenerator.generateKey()
    }

    /**
     * Deletes the key from the biometric-encrypted storage and the associated secret key from the keystore.
     */
    protected open fun deleteKey(key: String): Boolean {
        if (secretKeyStore.isKeyEntry(key)) {
            secretKeyStore.deleteEntry(key)
        }
        return encryptedStorage.delete(key)
    }
}
