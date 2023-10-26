package org.racehorse

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.io.Serializable
import java.security.KeyStore
import javax.crypto.Cipher
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

    private val biometricManager by lazy { BiometricManager.from(activity) }

    private val encryptedStorage = EncryptedStorage(storageDir)

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onSetBiometricEncryptedValue(event: SetBiometricEncryptedValueEvent) {
        val baseCipher = getCipher()
        baseCipher.init(Cipher.ENCRYPT_MODE, getSecretKey(event.key))

        authenticate(event.config, baseCipher) { cryptoObject ->
            val cipher = cryptoObject?.cipher

            val result =
                cipher != null && encryptedStorage.set(cipher, event.key, event.value.toByteArray(Charsets.UTF_8))

            event.respond(SetBiometricEncryptedValueEvent.ResultEvent(result))
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        val (iv, encryptedBytes) = encryptedStorage.getRecord(event.key)
            ?: return event.respond(GetEncryptedValueEvent.ResultEvent(null))

        val baseCipher = getCipher()
        baseCipher.init(Cipher.DECRYPT_MODE, getSecretKey(event.key), IvParameterSpec(iv))

        authenticate(event.config, baseCipher) { cryptoObject ->
            val cipher = cryptoObject?.cipher

            val value = cipher?.let { encryptedStorage.decrypt(cipher, encryptedBytes)?.toString(Charsets.UTF_8) }

            event.respond(GetEncryptedValueEvent.ResultEvent(value))
        }
    }

    @Subscribe
    open fun onHasBiometricEncryptedValue(event: HasBiometricEncryptedValueEvent) {
        event.respond(HasBiometricEncryptedValueEvent.ResultEvent(encryptedStorage.has(event.key)))
    }

    @Subscribe
    open fun onDeleteBiometricEncryptedValue(event: DeleteBiometricEncryptedValueEvent) {
        event.respond(DeleteBiometricEncryptedValueEvent.ResultEvent(encryptedStorage.delete(event.key)))
    }

    protected open fun authenticate(
        config: BiometricConfig?,
        cipher: Cipher,
        callback: (cryptoObject: BiometricPrompt.CryptoObject?) -> Unit
    ) {
        val authenticators = BiometricAuthenticator.from(config?.authenticators)

        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            callback(null)
            return
        }

        val authCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                callback(null)

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                callback(result.cryptoObject)

            override fun onAuthenticationFailed() =
                callback(null)
        }

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config?.title ?: "Authentication required")
            .setSubtitle(config?.subtitle)
            .setDescription(config?.description)
            .setAllowedAuthenticators(authenticators)

        if (config?.negativeButtonText != null && authenticators and BiometricManager.Authenticators.BIOMETRIC_WEAK == 0) {
            promptInfoBuilder.setNegativeButtonText(config.negativeButtonText)
        }

        try {
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), authCallback)

            prompt.authenticate(promptInfoBuilder.build(), BiometricPrompt.CryptoObject(cipher))
        } catch (e: Throwable) {
            e.printStackTrace()
            callback(null)
        }
    }

    protected open fun getCipher(): Cipher {
        return Cipher.getInstance("$ENCRYPTION_ALGORITHM/$ENCRYPTION_BLOCK_MODE/$ENCRYPTION_PADDING")
    }

    protected open fun getSecretKey(keyAlias: String): SecretKey {
        // Return existing key
        val keyStore = KeyStore.getInstance(SECRET_KEYSTORE_TYPE)
        keyStore.load(null)
        keyStore.getKey(keyAlias, null)?.let { return it as SecretKey }

        // Create a new key
        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(ENCRYPTION_BLOCK_MODE)
                .setEncryptionPaddings(ENCRYPTION_PADDING)
                .setUserAuthenticationRequired(true)
                .setKeySize(SECRET_KEY_SIZE)
                .setInvalidatedByBiometricEnrollment(true)
                .build()

        val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM, SECRET_KEYSTORE_TYPE)
        keyGenerator.init(keyGenParameterSpec)

        return keyGenerator.generateKey()
    }
}
