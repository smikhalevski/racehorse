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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.racehorse.utils.ifNullOrBlank
import org.racehorse.utils.sha256
import java.io.File
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

enum class BiometricStorageErrorCode(val value: Int) {
    @SerialName("unknown")
    UNKNOWN(-1),

    @SerialName("storage_failed")
    STORAGE_FAILED(-2),

    @SerialName("key_unrecoverable")
    KEY_UNRECOVERABLE(-3),

    @SerialName("hardware_unavailable")
    HARDWARE_UNAVAILABLE(BiometricPrompt.ERROR_HW_UNAVAILABLE),

    @SerialName("unable_to_process")
    UNABLE_TO_PROCESS(BiometricPrompt.ERROR_UNABLE_TO_PROCESS),

    @SerialName("timeout")
    TIMEOUT(BiometricPrompt.ERROR_TIMEOUT),

    @SerialName("no_space")
    NO_SPACE(BiometricPrompt.ERROR_NO_SPACE),

    @SerialName("canceled")
    CANCELED(BiometricPrompt.ERROR_CANCELED),

    @SerialName("lockout")
    LOCKOUT(BiometricPrompt.ERROR_LOCKOUT),

    @SerialName("vendor")
    VENDOR(BiometricPrompt.ERROR_VENDOR),

    @SerialName("lockout_permanent")
    LOCKOUT_PERMANENT(BiometricPrompt.ERROR_LOCKOUT_PERMANENT),

    @SerialName("user_canceled")
    USER_CANCELED(BiometricPrompt.ERROR_USER_CANCELED),

    @SerialName("no_biometrics")
    NO_BIOMETRICS(BiometricPrompt.ERROR_NO_BIOMETRICS),

    @SerialName("hardware_not_present")
    HARDWARE_NOT_PRESENT(BiometricPrompt.ERROR_HW_NOT_PRESENT),

    @SerialName("negative_button")
    NEGATIVE_BUTTON(BiometricPrompt.ERROR_NEGATIVE_BUTTON),

    @SerialName("no_device_credential")
    NO_DEVICE_CREDENTIAL(BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL),

    @SerialName("security_update_required")
    SECURITY_UPDATE_REQUIRED(BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED);

    companion object {
        fun from(value: Int) = BiometricStorageErrorCode.entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}

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

    @Serializable
    class ResultEvent(val isSuccessful: Boolean, val errorCode: BiometricStorageErrorCode?) : ResponseEvent() {

        constructor(errorCode: BiometricStorageErrorCode?) : this(errorCode == null, errorCode)
    }
}

/**
 * Retrieves a biometric encrypted value associated with the key.
 */
@Serializable
class GetBiometricEncryptedValueEvent(val key: String, val config: BiometricConfig? = null) : RequestEvent() {

    @Serializable
    class ResultEvent(val value: String?, val errorCode: BiometricStorageErrorCode?) : ResponseEvent() {

        constructor(value: String?) : this(value, null)

        constructor(errorCode: BiometricStorageErrorCode) : this(null, errorCode)
    }
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

        for (retryCount in 0..1) {
            try {
                val cipher = createEncryptCipher(event.key, event.config)
                encryptedStorage.set(cipher, event.key, valueBytes)

                event.respond(SetBiometricEncryptedValueEvent.ResultEvent(null))
                return
            } catch (e: Exception) {
                if (isTimeBoundAuthenticationRequired(e)) {
                    authenticate(null, event.config) { errorCode ->
                        event.respond {
                            SetBiometricEncryptedValueEvent.ResultEvent(
                                errorCode ?: try {
                                    val cipher = createEncryptCipher(event.key, event.config)
                                    encryptedStorage.set(cipher, event.key, valueBytes)
                                    null
                                } catch (_: IOException) {
                                    BiometricStorageErrorCode.STORAGE_FAILED
                                } catch (_: Throwable) {
                                    BiometricStorageErrorCode.UNKNOWN
                                }
                            )
                        }
                    }
                    return
                }

                if (isPerUseAuthenticationRequired(e)) {
                    val cipher = try {
                        createEncryptCipher(event.key, event.config)
                    } catch (_: Throwable) {
                        event.respond(SetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.KEY_UNRECOVERABLE))
                        return
                    }

                    authenticate(cipher, event.config) { errorCode ->
                        event.respond {
                            SetBiometricEncryptedValueEvent.ResultEvent(
                                errorCode ?: try {
                                    encryptedStorage.set(cipher, event.key, valueBytes)
                                    null
                                } catch (_: IOException) {
                                    BiometricStorageErrorCode.STORAGE_FAILED
                                } catch (_: Throwable) {
                                    BiometricStorageErrorCode.UNKNOWN
                                }
                            )
                        }
                    }
                    return
                }

                if (isKeyInvalidated(e)) {
                    deleteSecretKey(event.key)

                    if (retryCount == 0) {
                        continue
                    }
                    event.respond(SetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.KEY_UNRECOVERABLE))
                    return
                }

                // Unrecoverable key
                SetBiometricEncryptedValueEvent.ResultEvent(
                    if (e is IOException || e is BadPaddingException) BiometricStorageErrorCode.STORAGE_FAILED
                    else BiometricStorageErrorCode.UNKNOWN
                )
                return
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        val record = try {
            encryptedStorage.getRecord(event.key)
        } catch (_: IOException) {
            // Corrupted record
            return event.respond(GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.STORAGE_FAILED))
        }

        if (record == null) {
            return event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))
        }

        val ivParameterSpec = IvParameterSpec(record.iv)

        try {
            val cipher = createDecryptCipher(event.key, ivParameterSpec)
            val valueBytes = encryptedStorage.decrypt(cipher, record.encryptedValue)

            event.respond(GetBiometricEncryptedValueEvent.ResultEvent(valueBytes?.toString(Charsets.UTF_8)))
        } catch (e: Exception) {
            if (isTimeBoundAuthenticationRequired(e)) {
                authenticate(null, event.config) { errorCode ->
                    event.respond {
                        if (errorCode != null) {
                            GetBiometricEncryptedValueEvent.ResultEvent(errorCode)
                        } else try {
                            val cipher = createDecryptCipher(event.key, ivParameterSpec)
                            val valueBytes = encryptedStorage.decrypt(cipher, record.encryptedValue)

                            GetBiometricEncryptedValueEvent.ResultEvent(valueBytes?.toString(Charsets.UTF_8))
                        } catch (_: IOException) {
                            GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.STORAGE_FAILED)
                        } catch (_: BadPaddingException) {
                            GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.STORAGE_FAILED)
                        } catch (_: Throwable) {
                            GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.UNKNOWN)
                        }
                    }
                }
                return
            }

            if (isPerUseAuthenticationRequired(e)) {
                val cipher = try {
                    createDecryptCipher(event.key, ivParameterSpec)
                } catch (_: Throwable) {
                    event.respond(GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.KEY_UNRECOVERABLE))
                    return
                }

                authenticate(cipher, event.config) { errorCode ->
                    event.respond {
                        if (errorCode != null) {
                            GetBiometricEncryptedValueEvent.ResultEvent(errorCode)
                        } else try {
                            val valueBytes = encryptedStorage.decrypt(cipher, record.encryptedValue)

                            GetBiometricEncryptedValueEvent.ResultEvent(valueBytes?.toString(Charsets.UTF_8))
                        } catch (_: IOException) {
                            GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.STORAGE_FAILED)
                        } catch (_: BadPaddingException) {
                            GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.STORAGE_FAILED)
                        } catch (_: Throwable) {
                            GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.UNKNOWN)
                        }
                    }
                }
                return
            }

            if (isKeyInvalidated(e)) {
                event.respond(GetBiometricEncryptedValueEvent.ResultEvent(BiometricStorageErrorCode.KEY_UNRECOVERABLE))
                return
            }

            // Unrecoverable key
            SetBiometricEncryptedValueEvent.ResultEvent(
                if (e is IOException || e is BadPaddingException) BiometricStorageErrorCode.STORAGE_FAILED
                else BiometricStorageErrorCode.UNKNOWN
            )
            return
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
        callback: (errorCode: BiometricStorageErrorCode?) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed || activity.supportFragmentManager.isStateSaved) {
            callback(BiometricStorageErrorCode.UNKNOWN)
            return
        }

        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) {
                callback(BiometricStorageErrorCode.from(errorCode))
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                callback(null)
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

                callback(BiometricStorageErrorCode.LOCKOUT)
            }
        }
    }

    protected open fun toKeyAlias(key: String): String {
        return "racehorse://" + storageDir.canonicalPath.sha256().substring(0, 32) + key.sha256()
    }

    protected open fun createSecretKey(key: String, config: BiometricConfig?): SecretKey {
        val builder =
            KeyGenParameterSpec.Builder(toKeyAlias(key), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
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

    protected open fun getSecretKey(key: String): SecretKey? {
        return keyStore.getKey(toKeyAlias(key), null) as? SecretKey
    }

    protected open fun deleteSecretKey(key: String) {
        val keyAlias = toKeyAlias(key)

        if (keyStore.isKeyEntry(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }

    protected open fun createCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    private fun createEncryptCipher(key: String, config: BiometricConfig?): Cipher {
        val cipher = createCipher()
        val secretKey = getSecretKey(key) ?: createSecretKey(key, config)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        return cipher
    }

    private fun createDecryptCipher(key: String, ivParameterSpec: IvParameterSpec): Cipher {
        val cipher = createCipher()
        val secretKey = getSecretKey(key) ?: throw UnrecoverableKeyException()

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
