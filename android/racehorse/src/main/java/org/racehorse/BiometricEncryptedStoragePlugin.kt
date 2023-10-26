package org.racehorse

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.AuthenticationResult
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.math.BigInteger
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Associates a value with a key in a biometric encrypted storage.
 *
 * @param key A key to set. Must be a valid file name.
 * @param value A value to write to the file.
 */
class SetBiometricEncryptedValueEvent(val key: String, val value: String) : RequestEvent()

/**
 * Retrieves a biometric encrypted value associated with the key.
 */
class GetBiometricEncryptedValueEvent(val key: String) : RequestEvent() {

    /**
     * The deciphered value or `null` if key wasn't found or if password is incorrect.
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
 * Deletes a biometric encrypted value associated with the key.
 */
class DeleteBiometricEncryptedValueEvent(val key: String) : RequestEvent() {
    class ResultEvent(val deleted: Boolean) : ResponseEvent()
}

/**
 * A biometric encrypted key-value file-based storage.
 *
 * @param storageDir The directory to write files to.
 */
open class BiometricEncryptedStoragePlugin(private val authenticators: Int, private val activity: FragmentActivity, private val storageDir: File) {

    companion object {
        private const val CIPHER_ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val CIPHER_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val CIPHER_BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val SECRET_KEYSTORE_TYPE = "AndroidKeyStore"
        private const val SECRET_KEY_LENGTH = 256
        private const val KEY_HASH_ALGORITHM = "MD5"
        private const val VALUE_HASH_ALGORITHM = "SHA-512"
        private const val VALUE_HASH_LENGTH = 64 // bytes
        private const val IV_LENGTH = 16 // bytes
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onSetBiometricEncryptedValue(event: SetBiometricEncryptedValueEvent) {
        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            ExceptionEvent(IllegalStateException("Failed"))
            return
        }

        val valueBytes = event.value.toByteArray()

        val cipher = getCipher()
        val digest = MessageDigest.getInstance(VALUE_HASH_ALGORITHM)

        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey("event.password"))

        authenticate(cipher) { cryptoObject ->
            event.respond(
                if (cryptoObject != null) {
                    getFile(event.key).writeBytes(cipher.iv + cipher.doFinal(digest.digest(valueBytes) + valueBytes))
                    VoidEvent()
                } else {
                    ExceptionEvent(IllegalStateException("Failed"))
                }
            )
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    open fun onGetBiometricEncryptedValue(event: GetBiometricEncryptedValueEvent) {
        if (BiometricManager.from(activity).canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            ExceptionEvent(IllegalStateException("Failed"))
            return
        }

        val file = getFile(event.key)

        if (!file.exists()) {
            event.respond(GetBiometricEncryptedValueEvent.ResultEvent(null))
            return
        }

        val fileBytes = file.readBytes()

        val cipher = getCipher()

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey("event.password"),
            IvParameterSpec(fileBytes.copyOf(IV_LENGTH))
        )

        authenticate(cipher) { cryptoObject ->
            event.respond(
                if (cryptoObject != null) {
                    try {
                        val bytes = cipher.doFinal(fileBytes.copyOfRange(IV_LENGTH, fileBytes.size))

                        GetBiometricEncryptedValueEvent.ResultEvent(
                            String(bytes.copyOfRange(VALUE_HASH_LENGTH, bytes.size))
                        )
                    } catch (_: BadPaddingException) {
                        GetBiometricEncryptedValueEvent.ResultEvent(null)
                    } catch (e: Throwable) {
                        ExceptionEvent(e)
                    }
                } else {
                    ExceptionEvent(IllegalStateException("Failed"))
                }
            )
        }
    }

    @Subscribe
    open fun onHasBiometricEncryptedValue(event: HasBiometricEncryptedValueEvent) {
        event.respond(HasBiometricEncryptedValueEvent.ResultEvent(getFile(event.key).exists()))
    }

    @Subscribe
    open fun onDeleteBiometricEncryptedValue(event: DeleteBiometricEncryptedValueEvent) {
        event.respond(DeleteBiometricEncryptedValueEvent.ResultEvent(getFile(event.key).delete()))
    }

    protected open fun authenticate(cipher: Cipher, listener: (cryptoObject: CryptoObject?) -> Unit) {
        val callback = object : AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = listener(null)

            override fun onAuthenticationSucceeded(result: AuthenticationResult) = listener(result.cryptoObject)

            override fun onAuthenticationFailed() = listener(null)
        }

        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(
            PromptInfo.Builder()
                .setTitle("event.title")
                .setSubtitle("event.subtitle")
                .setNegativeButtonText("event.negativeButtonText")
                .setAllowedAuthenticators(authenticators)
                .build(),
            CryptoObject(cipher)
        )
    }

    protected open fun getCipher(): Cipher {
        return Cipher.getInstance("$CIPHER_ALGORITHM/$CIPHER_BLOCK_MODE/$CIPHER_PADDING")
    }

    protected open fun getOrCreateSecretKey(keyAlias: String): SecretKey {
        val keyStore = KeyStore.getInstance(SECRET_KEYSTORE_TYPE)
        keyStore.load(null)

        val key = keyStore.getKey(keyAlias, null)
        if (key is SecretKey) {
            return key
        }

        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(CIPHER_BLOCK_MODE)
                .setEncryptionPaddings(CIPHER_PADDING)
                .setUserAuthenticationRequired(true)
                .setKeySize(SECRET_KEY_LENGTH)
                .setInvalidatedByBiometricEnrollment(true)
                .build()

        val keyGenerator = KeyGenerator.getInstance(CIPHER_ALGORITHM, SECRET_KEYSTORE_TYPE)
        keyGenerator.init(keyGenParameterSpec)

        return keyGenerator.generateKey()
    }

    /**
     * Returns the file descriptor for a given key.
     */
    private fun getFile(key: String): File {
        storageDir.mkdirs()

        val keyHash = MessageDigest.getInstance(KEY_HASH_ALGORITHM).digest(key.toByteArray())

        return File(storageDir, BigInteger(1, keyHash).toString(16))
    }
}
