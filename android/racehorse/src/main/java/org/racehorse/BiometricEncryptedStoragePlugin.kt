package org.racehorse

import android.annotation.SuppressLint
import android.os.Build
import android.security.KeyStoreException
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricFragment
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.ifNullOrBlank
import java.io.File
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@Serializable
class BiometricConfig(
    val title: String? = null,
    val subtitle: String? = null,
    val description: String? = null,
    val negativeButtonText: String? = null,
    val authenticators: Array<BiometricAuthenticator>? = null,
    val authenticationValidityDuration: Int? = null
)

/**
 * Associates a value with a key in a biometric encrypted storage.
 *
 * @param key A key to set. Must be a valid file name.
 * @param value A value to write to the file.
 * @param config Options of the prompt shown to the user.
 */
@Serializable
class SetBiometricEncryptedValueEvent(
    val key: String,
    val value: String,
    val config: BiometricConfig? = null
) : RequestEvent() {

    /**
     * @param isSuccessful `true` if the value was written to the storage, or `false` otherwise.
     */
    @Serializable
    class ResultEvent(val isSuccessful: Boolean) : ResponseEvent()
}

/**
 * Retrieves a biometric encrypted value associated with the key.
 */
@Serializable
class GetBiometricEncryptedValueEvent(val key: String, val config: BiometricConfig? = null) : RequestEvent() {

    /**
     * @param value The deciphered value or `null` if key wasn't found or auth has failed.
     */
    @Serializable
    class ResultEvent(val value: String?) : ResponseEvent()
}

/**
 * Checks that the key exists in the storage.
 */
@Serializable
class HasBiometricEncryptedValueEvent(val key: String) : RequestEvent() {

    @Serializable
    class ResultEvent(val isExisting: Boolean) : ResponseEvent()
}

/**
 * Deletes a value associated with the key.
 */
@Serializable
class DeleteBiometricEncryptedValueEvent(val key: String) : RequestEvent() {

    @Serializable
    class ResultEvent(val isDeleted: Boolean) : ResponseEvent()
}

/**
 * A biometric encrypted key-value file-based storage.
 *
 * [Authenticate using only biometric credentials.](https://developer.android.com/training/sign-in/biometric-auth#biometric-only).
 *
 * @param activity The activity used to show the biometric prompt.
 * @param storageDir The directory to write encrypted files to.
 */
open class BiometricEncryptedStoragePlugin(private val activity: FragmentActivity, private val storageDir: File) {

    protected companion object {
        const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        const val SECRET_KEYSTORE_TYPE = "AndroidKeyStore"
        const val SECRET_KEY_SIZE = 256

        /**
         * @see BiometricPrompt.BIOMETRIC_FRAGMENT_TAG
         */
        const val BIOMETRIC_FRAGMENT_TAG = "androidx.biometric.BiometricFragment"

        /**
         * @see BiometricFragment.FINGERPRINT_DIALOG_FRAGMENT_TAG
         */
        const val FINGERPRINT_DIALOG_FRAGMENT_TAG = "androidx.biometric.FingerprintDialogFragment"
    }

    private val keyStore by lazy { KeyStore.getInstance(SECRET_KEYSTORE_TYPE).apply { load(null) } }

    private val encryptedStorage = EncryptedStorage(storageDir)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onSetBiometricEncryptedValue(event: SetBiometricEncryptedValueEvent) {
        val valueBytes = event.value.toByteArray(Charsets.UTF_8)

        for (i in 0..1) {
            try {
                val cipher = createEncryptCypher(event.key, event.config)
                val isSuccessful = encryptedStorage.set(cipher, event.key, valueBytes)

                event.respond(SetBiometricEncryptedValueEvent.ResultEvent(isSuccessful))
                break
            } catch (e: Exception) {
                if (isTimeBoundAuthenticationRequired(e)) {
                    authenticate(null, event.config) { isAuthenticated ->
                        event.respond {
                            if (!isAuthenticated) {
                                return@respond SetBiometricEncryptedValueEvent.ResultEvent(false)
                            }

                            val cipher = createEncryptCypher(event.key, event.config)
                            val isSuccessful = encryptedStorage.set(cipher, event.key, valueBytes)

                            SetBiometricEncryptedValueEvent.ResultEvent(isSuccessful)
                        }
                    }
                    break
                }

                if (isPerUseAuthenticationRequired(e)) {
                    val cipher = createEncryptCypher(event.key, event.config)

                    authenticate(cipher, event.config) { isAuthenticated ->
                        event.respond {
                            if (!isAuthenticated) {
                                return@respond SetBiometricEncryptedValueEvent.ResultEvent(false)
                            }

                            val isSuccessful = encryptedStorage.set(cipher, event.key, valueBytes)

                            SetBiometricEncryptedValueEvent.ResultEvent(isSuccessful)
                        }
                    }
                    break
                }

                if (isKeyInvalidated(e)) {
                    deleteSecretKey(event.key)

                    if (i == 1) {
                        event.respond(SetBiometricEncryptedValueEvent.ResultEvent(false))
                    }
                    continue
                }

                // Unexpected exception
                throw e
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        val record = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))

        val ivParameterSpec = IvParameterSpec(record.iv)

        try {
            val cipher = createDecryptCypher(event.key, ivParameterSpec)
                ?: return event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))

            val value = encryptedStorage.decrypt(cipher, record.encryptedValue)?.toString(Charsets.UTF_8)

            event.respond(GetBiometricEncryptedValueEvent.ResultEvent(value))
        } catch (e: Exception) {
            if (isTimeBoundAuthenticationRequired(e)) {
                authenticate(null, event.config) { isAuthenticated ->
                    event.respond {
                        if (!isAuthenticated) {
                            return@respond GetBiometricEncryptedValueEvent.ResultEvent(null)
                        }

                        val cipher = createDecryptCypher(event.key, ivParameterSpec)
                            ?: return@respond GetBiometricEncryptedValueEvent.ResultEvent(null)

                        val value = encryptedStorage.decrypt(cipher, record.encryptedValue)?.toString(Charsets.UTF_8)

                        GetBiometricEncryptedValueEvent.ResultEvent(value)
                    }
                }
            }

            if (isPerUseAuthenticationRequired(e)) {
                val cipher = createDecryptCypher(event.key, ivParameterSpec)
                    ?: return event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))

                authenticate(cipher, event.config) { isAuthenticated ->
                    event.respond {
                        if (!isAuthenticated) {
                            return@respond GetBiometricEncryptedValueEvent.ResultEvent(null)
                        }

                        val value = encryptedStorage.decrypt(cipher, record.encryptedValue)?.toString(Charsets.UTF_8)

                        GetBiometricEncryptedValueEvent.ResultEvent(value)
                    }
                }
                return
            }

            if (isKeyInvalidated(e)) {
                event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))
                return
            }

            // Unexpected exception
            throw e
        }
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    open fun onHasBiometricEncryptedValue(event: HasBiometricEncryptedValueEvent) {
        event.respond(HasBiometricEncryptedValueEvent.ResultEvent(encryptedStorage.has(event.key)))
    }

    @Subscribe(threadMode = ThreadMode.POSTING)
    open fun onDeleteBiometricEncryptedValue(event: DeleteBiometricEncryptedValueEvent) {
        deleteSecretKey(event.key)
        event.respond(DeleteBiometricEncryptedValueEvent.ResultEvent(encryptedStorage.delete(event.key)))
    }

    private fun authenticate(
        cipher: Cipher?,
        config: BiometricConfig?,
        callback: (isAuthenticated: Boolean) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed || activity.supportFragmentManager.isStateSaved) {
            callback(false)
            return
        }

        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) {
                callback(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                callback(true)
            }

            // User can retry, do nothing
            override fun onAuthenticationFailed() {}
        }

        val authenticators = BiometricAuthenticator.from(config?.authenticators)

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config?.title.ifNullOrBlank { "Authentication required" })
            .setSubtitle(config?.subtitle)
            .setDescription(config?.description)
            .setAllowedAuthenticators(authenticators)

        // Negative button only needed if device credential is not allowed
        if (authenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL == 0) {
            builder.setNegativeButtonText(config?.negativeButtonText.ifNullOrBlank { "Cancel" })
        }

        activity.runOnUiThread {
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), promptCallback)

            if (cipher != null) {
                prompt.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
            } else {
                prompt.authenticate(builder.build())
            }

            if (!isBiometricPromptShown()) {
                // Make sure that biometric authentication was not prevented by user lockout
                // after too many failed attempts
                // @see https://issuetracker.google.com/issues/277499446

                callback(false)
            }
        }
    }

    protected open fun createSecretKey(key: String, config: BiometricConfig?): SecretKey {
        val builder = KeyGenParameterSpec.Builder(key, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setKeySize(SECRET_KEY_SIZE)

        val validityDuration = config?.authenticationValidityDuration ?: -1

        if (validityDuration >= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    validityDuration,
                    KeyProperties.AUTH_DEVICE_CREDENTIAL or KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(validityDuration)
            }
        }

        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, SECRET_KEYSTORE_TYPE)
        keyGenerator.init(builder.build())

        return keyGenerator.generateKey()
    }

    protected open fun getSecretKey(key: String) = keyStore.getKey(key, null) as? SecretKey

    protected open fun deleteSecretKey(key: String) {
        if (keyStore.isKeyEntry(key)) {
            keyStore.deleteEntry(key)
        }
    }

    protected open fun createCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    private fun createEncryptCypher(key: String, config: BiometricConfig?): Cipher {
        val cipher = createCipher()
        val secretKey = getSecretKey(key) ?: createSecretKey(key, config)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher
    }

    private fun createDecryptCypher(key: String, ivParameterSpec: IvParameterSpec): Cipher? {
        val cipher = createCipher()
        val secretKey = getSecretKey(key) ?: return null

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)

        return cipher
    }

    /**
     * Returns `true` if authentication must be passed _without_ the cipher.
     */
    private fun isTimeBoundAuthenticationRequired(e: Exception): Boolean {
        return e is UserNotAuthenticatedException
    }

    /**
     * Returns `true` if authentication must be passed _with_ the initialized cipher.
     */
    // Suppresses android.security.KeyStoreException warning (class exists since API level 18)
    @SuppressLint("NewApi")
    private fun isPerUseAuthenticationRequired(e: Exception): Boolean {
        val cause = e as? KeyStoreException ?: e.cause as? KeyStoreException ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            cause.numericErrorCode == KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED
        } else {
            cause.message == "Key user not authenticated"
        }
    }

    /**
     * Returns `true` if the secret key was invalidated an should not be used.
     */
    // Suppresses android.security.KeyStoreException warning (class exists since API level 18)
    @SuppressLint("NewApi")
    private fun isKeyInvalidated(e: Exception): Boolean {
        return e is InvalidKeyException ||
            e is InvalidAlgorithmParameterException ||
            e is UnrecoverableKeyException ||
            e is NoSuchAlgorithmException ||
            e is KeyStoreException
    }

    private fun isBiometricPromptShown(): Boolean {
        return activity.supportFragmentManager.findFragmentByTag(BIOMETRIC_FRAGMENT_TAG) != null ||
            activity.supportFragmentManager.findFragmentByTag(FINGERPRINT_DIALOG_FRAGMENT_TAG) != null
    }
}
