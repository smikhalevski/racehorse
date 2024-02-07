package org.racehorse

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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
    class ResultEvent(val isExisting: Boolean) : ResponseEvent()
}

/**
 * Deletes a value associated with the key.
 */
class DeleteBiometricEncryptedValueEvent(val key: String) : RequestEvent() {
    class ResultEvent(val isDeleted: Boolean) : ResponseEvent()
}

/**
 * A biometric encrypted key-value file-based storage.
 *
 * @param storageDir The directory to write files to.
 */
open class BiometricEncryptedStoragePlugin(private val activity: FragmentActivity, private val storageDir: File) {

    companion object {
        protected const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        protected const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        protected const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        protected const val SECRET_KEYSTORE_TYPE = "AndroidKeyStore"
        protected const val SECRET_KEY_SIZE = 256
    }

    private val keyStore by lazy { KeyStore.getInstance(SECRET_KEYSTORE_TYPE).apply { load(null) } }

    private val biometricManager by lazy { BiometricManager.from(activity) }

    private val encryptedStorage = EncryptedStorage(storageDir)

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onSetBiometricEncryptedValue(event: SetBiometricEncryptedValueEvent) {
        if (!isBiometricEnrolled(event.config?.authenticators)) {
            event.respond(SetBiometricEncryptedValueEvent.ResultEvent(false))
            return
        }

        var secretKey = getSecretKey(event.key) ?: createSecretKey(event.key)

        val cipher = createCipher()

        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        } catch (_: KeyPermanentlyInvalidatedException) {
            // The key was invalidated due to a biometric enrollment, and storage entry became unrecoverable
            deleteEntry(event.key)
            secretKey = createSecretKey(event.key)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        } catch (_: UserNotAuthenticatedException) {
            // The key uses authentication validity duration, and authentication has expired
            return authenticate(event.config, null) { isAuthenticated ->
                event.respond {
                    SetBiometricEncryptedValueEvent.ResultEvent(
                        if (isAuthenticated) {
                            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                            encrypt(cipher, event.key, event.value)
                        } else false
                    )
                }
            }
        }

        try {
            // Maybe authentication hasn't expired yet
            event.respond(SetBiometricEncryptedValueEvent.ResultEvent(encrypt(cipher, event.key, event.value)))
        } catch (_: IllegalBlockSizeException) {
            // Authentication has expired or it is required for every key use
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            authenticate(event.config, cipher) { isAuthenticated ->
                event.respond {
                    SetBiometricEncryptedValueEvent.ResultEvent(
                        isAuthenticated && encrypt(cipher, event.key, event.value)
                    )
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        if (!isBiometricEnrolled(event.config?.authenticators)) {
            event.respond(GetEncryptedValueEvent.ResultEvent(null))
            return
        }

        val record = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetEncryptedValueEvent.ResultEvent(null))

        val secretKey = getSecretKey(event.key)

        if (secretKey == null) {
            // The key was somehow deleted, and storage entry became unrecoverable
            deleteEntry(event.key)
            event.respond(GetEncryptedValueEvent.ResultEvent(null))
            return
        }

        val cipher = createCipher()
        val params = IvParameterSpec(record.iv)

        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, params)
        } catch (_: KeyPermanentlyInvalidatedException) {
            // The key was invalidated due to a biometric enrollment, and storage entry became unrecoverable
            deleteEntry(event.key)
            event.respond(GetEncryptedValueEvent.ResultEvent(null))
            return
        } catch (_: UserNotAuthenticatedException) {
            // The key uses authentication validity duration, and authentication has expired
            authenticate(event.config, null) { isAuthenticated ->
                event.respond {
                    GetEncryptedValueEvent.ResultEvent(
                        if (isAuthenticated) {
                            cipher.init(Cipher.DECRYPT_MODE, secretKey, params)
                            decrypt(cipher, record)
                        } else null
                    )
                }
            }
            return
        }

        try {
            // Maybe authentication hasn't expired yet
            event.respond(GetEncryptedValueEvent.ResultEvent(decrypt(cipher, record)))
        } catch (_: IllegalBlockSizeException) {
            // Authentication has expired or it is required for every key use
            cipher.init(Cipher.DECRYPT_MODE, secretKey, params)

            authenticate(event.config, cipher) { isAuthenticated ->
                event.respond {
                    GetEncryptedValueEvent.ResultEvent(if (isAuthenticated) decrypt(cipher, record) else null)
                }
            }
        }
    }

    @Subscribe
    open fun onHasBiometricEncryptedValue(event: HasBiometricEncryptedValueEvent) {
        event.respond(HasBiometricEncryptedValueEvent.ResultEvent(encryptedStorage.has(event.key)))
    }

    @Subscribe
    open fun onDeleteBiometricEncryptedValue(event: DeleteBiometricEncryptedValueEvent) {
        event.respond(DeleteBiometricEncryptedValueEvent.ResultEvent(deleteEntry(event.key)))
    }

    protected open fun authenticate(
        config: BiometricConfig?,
        cipher: Cipher?,
        callback: (isAuthenticated: Boolean) -> Unit
    ) {
        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) = callback(false)

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = callback(true)

            override fun onAuthenticationFailed() {}
        }

        val authenticators = BiometricAuthenticator.from(config?.authenticators)

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config?.title.ifNullOrBlank { "Authentication required" })
            .setSubtitle(config?.subtitle)
            .setDescription(config?.description)
            .setAllowedAuthenticators(authenticators)

        if (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
            builder.setNegativeButtonText(config?.negativeButtonText.ifNullOrBlank { "Cancel" })
        }

        val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), promptCallback)

        if (cipher == null) {
            prompt.authenticate(builder.build())
        } else {
            prompt.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
        }
    }

    protected open fun deleteEntry(key: String): Boolean {
        if (keyStore.isKeyEntry(key)) {
            keyStore.deleteEntry(key)
        }
        return encryptedStorage.delete(key)
    }

    protected open fun createCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    protected open fun createSecretKey(key: String): SecretKey {
        val builder = KeyGenParameterSpec.Builder(key, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)

        configureSecretKey(key, builder)

        builder
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setKeySize(SECRET_KEY_SIZE)

        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, SECRET_KEYSTORE_TYPE)
        keyGenerator.init(builder.build())

        return keyGenerator.generateKey()
    }

    protected open fun configureSecretKey(key: String, builder: KeyGenParameterSpec.Builder) {
        builder.setUserAuthenticationRequired(true)
    }

    protected open fun getSecretKey(key: String) = keyStore.getKey(key, null) as? SecretKey

    private fun isBiometricEnrolled(authenticators: Array<BiometricAuthenticator>?) =
        biometricManager.canAuthenticate(BiometricAuthenticator.from(authenticators)) == BiometricManager.BIOMETRIC_SUCCESS

    private fun encrypt(cipher: Cipher, key: String, value: String) =
        encryptedStorage.set(cipher, key, value.toByteArray(Charsets.UTF_8))

    private fun decrypt(cipher: Cipher, record: EncryptedRecord) =
        encryptedStorage.decrypt(cipher, record.encryptedValue)?.toString(Charsets.UTF_8)
}
