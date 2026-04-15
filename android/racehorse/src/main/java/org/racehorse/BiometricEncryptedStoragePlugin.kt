package org.racehorse

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
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
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import java.util.concurrent.Executors
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
    }

    private val keyStore by lazy { KeyStore.getInstance(SECRET_KEYSTORE_TYPE).apply { load(null) } }

    private val encryptedStorage = EncryptedStorage(storageDir)

    private val executor = Executors.newSingleThreadExecutor()

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onSetBiometricEncryptedValue(event: SetBiometricEncryptedValueEvent) {
        val valueBytes = event.value.toByteArray(Charsets.UTF_8)

        for (i in 0..1) {
            val cipher = createCipher()

            try {
                val secretKey = getSecretKey(event.key) ?: createSecretKey(event.key, event.config)

                cipher.init(Cipher.ENCRYPT_MODE, secretKey)

                event.respond(
                    SetBiometricEncryptedValueEvent.ResultEvent(
                        encryptedStorage.set(cipher, event.key, valueBytes)
                    )
                )
                break
            } catch (_: UserNotAuthenticatedException) {
                // Need authentication (device credential or biometric)
                // https://developer.android.com/training/sign-in/biometric-auth#biometric-or-lock-screen
                authenticate(cipher, event.config) { authenticatedCipher ->
                    executor.submit {
                        event.respond {
                            SetBiometricEncryptedValueEvent.ResultEvent(
                                authenticatedCipher != null &&
                                    encryptedStorage.set(authenticatedCipher, event.key, valueBytes)
                            )
                        }
                    }
                }
                break
            } catch (e: Exception) {
                if (
                    e is InvalidKeyException ||
                    e is UnrecoverableKeyException ||
                    e is KeyStoreException ||
                    e is NoSuchAlgorithmException
                ) {
                    deleteSecretKey(event.key)

                    if (i == 1) {
                        event.respond(SetBiometricEncryptedValueEvent.ResultEvent(false))
                    }
                    continue
                }
                throw e
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        val cipher = createCipher()

        val record = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))

        try {
            val secretKey = getSecretKey(event.key)
                ?: return event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))

            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(record.iv))

            event.respond(
                GetBiometricEncryptedValueEvent.ResultEvent(
                    encryptedStorage.decrypt(cipher, record.encryptedValue)?.toString(Charsets.UTF_8)
                )
            )
        } catch (_: UserNotAuthenticatedException) {
            authenticate(cipher, event.config) { authenticatedCipher ->
                executor.submit {
                    event.respond {
                        GetBiometricEncryptedValueEvent.ResultEvent(
                            if (authenticatedCipher != null) {
                                encryptedStorage.decrypt(authenticatedCipher, record.encryptedValue)
                                    ?.toString(Charsets.UTF_8)
                            } else null
                        )
                    }
                }
            }
        } catch (e: Exception) {
            if (
                e is InvalidKeyException ||
                e is InvalidAlgorithmParameterException ||
                e is UnrecoverableKeyException ||
                e is KeyStoreException ||
                e is NoSuchAlgorithmException
            ) {
                event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))
                return
            }
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

    open fun close() {
        executor.shutdown()
    }

    private fun authenticate(
        cipher: Cipher,
        config: BiometricConfig?,
        callback: (authenticatedCipher: Cipher?) -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed || activity.supportFragmentManager.isStateSaved) {
            callback(null)
            return
        }

        val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) =
                callback(null)

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                callback(result.cryptoObject?.cipher)

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
            BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), promptCallback)
                .authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
        }
    }

    protected open fun createCipher() =
        Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")

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
}
