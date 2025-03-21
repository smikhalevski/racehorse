package org.racehorse

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.ifNullOrBlank
import java.io.File
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
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
 * @param storageDir The directory to write files to.
 */
open class BiometricEncryptedStoragePlugin(private val activity: FragmentActivity, private val storageDir: File) {

    protected companion object {
        const val ENCRYPTION_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        const val ENCRYPTION_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        const val ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        const val SECRET_KEYSTORE_TYPE = "AndroidKeyStore"
        const val SECRET_KEY_SIZE = 256
    }

    private val keyStore by lazy { KeyStore.getInstance(SECRET_KEYSTORE_TYPE).apply { load(null) } }

    private val encryptedStorage = EncryptedStorage(storageDir)

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onSetBiometricEncryptedValue(event: SetBiometricEncryptedValueEvent) {
        val cipher = createCipher()

        for (i in 1..2) {
            try {
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(event.key) ?: createSecretKey(event.key, event.config))
                event.respond(SetBiometricEncryptedValueEvent.ResultEvent(encrypt(cipher, event)))
                return
            } catch (e: IllegalBlockSizeException) {
                checkKeyUserNotAuthenticated(e)

                // https://developer.android.com/training/sign-in/biometric-auth#biometric-only
                cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(event.key))

                authenticate(cipher, event.config) { isAuthenticated ->
                    event.respond {
                        SetBiometricEncryptedValueEvent.ResultEvent(
                            if (isAuthenticated) {
                                encrypt(cipher, event)
                            } else false
                        )
                    }
                }
                return
            } catch (_: UserNotAuthenticatedException) {
                // https://developer.android.com/training/sign-in/biometric-auth#biometric-or-lock-screen
                val secretKey = getSecretKey(event.key)

                authenticate(null, event.config) { isAuthenticated ->
                    event.respond {
                        SetBiometricEncryptedValueEvent.ResultEvent(
                            if (isAuthenticated) {
                                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                                encrypt(cipher, event)
                            } else false
                        )
                    }
                }
                return
            } catch (e: KeyPermanentlyInvalidatedException) {
                deleteSecretKey(event.key)
            } catch (e: UnrecoverableKeyException) {
                deleteSecretKey(event.key)
            }
        }

        error("Cannot authenticate user")
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        val record = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetEncryptedValueEvent.ResultEvent(null))

        val secretKey = getSecretKey(event.key)
            ?: return event.respond(ExceptionEvent(UnrecoverableKeyException("Secret key not found")))

        val cipher = createCipher()
        val params = IvParameterSpec(record.iv)

        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, params)
            event.respond(GetEncryptedValueEvent.ResultEvent(decrypt(cipher, record)))
        } catch (e: IllegalBlockSizeException) {
            checkKeyUserNotAuthenticated(e)

            // https://developer.android.com/training/sign-in/biometric-auth#biometric-only
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(event.key), params)

            authenticate(cipher, event.config) { isAuthenticated ->
                event.respond {
                    GetEncryptedValueEvent.ResultEvent(
                        if (isAuthenticated) {
                            decrypt(cipher, record)
                        } else null
                    )
                }
            }
        } catch (_: UserNotAuthenticatedException) {
            // https://developer.android.com/training/sign-in/biometric-auth#biometric-or-lock-screen
            authenticate(null, event.config) { isAuthenticated ->
                event.respond {
                    GetEncryptedValueEvent.ResultEvent(
                        if (isAuthenticated) {
                            cipher.init(Cipher.DECRYPT_MODE, secretKey, params)
                            decrypt(cipher, record)
                        } else null
                    )
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
        deleteSecretKey(event.key)
        event.respond(DeleteBiometricEncryptedValueEvent.ResultEvent(encryptedStorage.delete(event.key)))
    }

    private fun authenticate(
        cipher: Cipher?,
        config: BiometricConfig?,
        callback: (isAuthenticated: Boolean) -> Unit
    ) {
        // Biometric authentication cannot be started after onSaveInstanceState() invocation
        // https://android.googlesource.com/platform/frameworks/support/+/f2e05c341382db64d127118a13451dcaa554b702/biometric/biometric/src/main/java/androidx/biometric/BiometricPrompt.java#962
        if (activity.supportFragmentManager.isStateSaved) {
            callback(false)
            return
        }

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

        // Make sure that biometric authentication was not prevented by user lockout
        //
        // @see https://issuetracker.google.com/issues/277499446
        // @see BiometricPrompt.BIOMETRIC_FRAGMENT_TAG
        // @see BiometricFragment.FINGERPRINT_DIALOG_FRAGMENT_TAG
        if (
            activity.supportFragmentManager.findFragmentByTag("androidx.biometric.BiometricFragment") == null &&
            activity.supportFragmentManager.findFragmentByTag("androidx.biometric.FingerprintDialogFragment") == null
        ) {
            callback(false)
        }
    }

    protected open fun createCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    protected open fun createSecretKey(key: String, config: BiometricConfig?): SecretKey {
        val builder = KeyGenParameterSpec.Builder(key, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .setBlockModes(ENCRYPTION_BLOCK_MODE)
            .setEncryptionPaddings(ENCRYPTION_PADDING)
            .setKeySize(SECRET_KEY_SIZE)

        val authenticationValidityDuration = config?.authenticationValidityDuration ?: -1

        if (authenticationValidityDuration >= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(
                    authenticationValidityDuration,
                    KeyProperties.AUTH_DEVICE_CREDENTIAL or KeyProperties.AUTH_BIOMETRIC_STRONG
                )
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(authenticationValidityDuration)
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

    private fun decrypt(cipher: Cipher, record: EncryptedRecord) =
        encryptedStorage.decrypt(cipher, record.encryptedValue)?.toString(Charsets.UTF_8)

    private fun encrypt(cipher: Cipher, event: SetBiometricEncryptedValueEvent) =
        encryptedStorage.set(cipher, event.key, event.value.toByteArray(Charsets.UTF_8))

    private fun checkKeyUserNotAuthenticated(e: Throwable) {
        if (e.cause?.message?.contains("Key user not authenticated") != true) {
            throw e
        }
    }
}
