package org.racehorse

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class EncryptedStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storageDir: File
    private lateinit var storage: EncryptedStorage
    private lateinit var secretKey: SecretKey

    @Before
    fun setUp() {
        storageDir = tempFolder.root
        storage = EncryptedStorage(storageDir)
        secretKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    }

    private fun getEncryptCipher(): Cipher =
        Cipher.getInstance("AES/CBC/PKCS5Padding").also {
            it.init(Cipher.ENCRYPT_MODE, secretKey)
        }

    private fun getDecryptCipher(iv: ByteArray): Cipher =
        Cipher.getInstance("AES/CBC/PKCS5Padding").also {
            it.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        }

    @Test
    fun `set returns true and creates a file`() {
        val result = storage.set(getEncryptCipher(), "key", "value".toByteArray())

        assertTrue(result)
        assertTrue(storageDir.listFiles()!!.isNotEmpty())
    }

    @Test
    fun `set overwrites an existing entry`() {
        storage.set(getEncryptCipher(), "key", "value1".toByteArray())
        storage.set(getEncryptCipher(), "key", "value2".toByteArray())

        val record = storage.getRecord("key")!!
        val value = storage.decrypt(getDecryptCipher(record.iv), record.encryptedValue)

        assertNotNull(value)
        assertEquals("value2", value!!.toString(Charsets.UTF_8))
    }

    @Test
    fun `set creates storageDir if it does not exist`() {
        val storageDir = File(tempFolder.root, "aaa/bbb/ccc")
        val encryptedStorage = EncryptedStorage(storageDir)

        val result = encryptedStorage.set(getEncryptCipher(), "key", "value".toByteArray())

        assertTrue(result)
        assertTrue(storageDir.exists())
    }

    @Test
    fun `has returns false for a missing key`() {
        assertFalse(storage.has("key"))
    }

    @Test
    fun `has returns true after set`() {
        storage.set(getEncryptCipher(), "key", "value".toByteArray())

        assertTrue(storage.has("key"))
    }

    @Test
    fun `has returns false after delete`() {
        storage.set(getEncryptCipher(), "key", "value".toByteArray())
        storage.delete("key")

        assertFalse(storage.has("key"))
    }

    @Test
    fun `getRecord returns null for missing key`() {
        assertNull(storage.getRecord("key"))
    }

    @Test
    fun `getRecord returns record with non-empty IV and encrypted value`() {
        storage.set(getEncryptCipher(), "key", "value".toByteArray())

        val record = storage.getRecord("key")

        assertNotNull(record)
        assertTrue(record!!.iv.isNotEmpty())
        assertTrue(record.encryptedValue.isNotEmpty())
    }

    @Test
    fun `getRecord returns null for an empty file`() {
        storage.getFile("key").writeBytes(ByteArray(0))

        assertNull(storage.getRecord("key"))
    }

    @Test
    fun `getRecord returns null for a file shorter than IV_LENGTH_SIZE`() {
        storage.getFile("key").writeBytes(ByteArray(3))

        assertNull(storage.getRecord("key"))
    }

    @Test
    fun `getRecord returns null for a file with bogus IV length`() {
        // Write a 4-byte header claiming an IV of 999 bytes, then nothing
        val buffer = ByteBuffer.allocate(4).putInt(999).array()

        storage.getFile("key").writeBytes(buffer)

        assertNull(storage.getRecord("key"))
    }

    @Test
    fun `getRecord returns null for an unreadable file`() {
        val file = storage.getFile("key")

        storage.set(getEncryptCipher(), "key", "value".toByteArray())

        file.setReadable(false)

        try {
            assertNull(storage.getRecord("key"))
        } finally {
            file.setReadable(true)
        }
    }

    @Test
    fun `delete returns true and removes the file`() {
        storage.set(getEncryptCipher(), "key", "value".toByteArray())

        assertTrue(storage.delete("key"))
        assertFalse(storage.has("key"))
    }

    @Test
    fun `delete returns false for a missing key`() {
        assertFalse(storage.delete("key"))
    }

    @Test
    fun `decrypt returns original value after round-trip`() {
        storage.set(getEncryptCipher(), "key", "value".toByteArray())

        val record = storage.getRecord("key")!!
        val result = storage.decrypt(getDecryptCipher(record.iv), record.encryptedValue)

        assertNotNull(result)
        assertEquals("value", result!!.toString(Charsets.UTF_8))
    }

    @Test
    fun `decrypt returns null when ciphertext is tampered`() {
        storage.set(getEncryptCipher(), "key", "value".toByteArray())

        val record = storage.getRecord("key")!!
        val tampered = record.encryptedValue.copyOf()

        tampered[tampered.size - 1] = tampered[tampered.size - 1].inc()

        val result = storage.decrypt(getDecryptCipher(record.iv), tampered)

        assertNull(result)
    }

    @Test
    fun `decrypt returns null when decrypted with the wrong key`() {
        storage.set(getEncryptCipher(), "key", "value".toByteArray())

        val record = storage.getRecord("key")!!

        val wrongKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

        val wrongCipher = Cipher.getInstance("AES/CBC/PKCS5Padding").also {
            it.init(Cipher.DECRYPT_MODE, wrongKey, IvParameterSpec(record.iv))
        }

        val result = storage.decrypt(wrongCipher, record.encryptedValue)

        assertNull(result)
    }

    @Test
    fun `decrypt returns null when ciphertext is too short to contain a hash`() {
        // Encrypt a payload that is shorter than HASH_SIZE bytes after decryption
        val cipher = getEncryptCipher()
        val encryptedValue = cipher.doFinal(ByteArray(4))

        val result = storage.decrypt(getDecryptCipher(cipher.iv), encryptedValue)

        assertNull(result)
    }

    @Test
    fun `different keys are stored independently`() {
        storage.set(getEncryptCipher(), "key1", "value1".toByteArray())
        storage.set(getEncryptCipher(), "key2", "value2".toByteArray())

        val record1 = storage.getRecord("key1")!!
        val record2 = storage.getRecord("key2")!!

        assertEquals(
            "value1",
            storage.decrypt(getDecryptCipher(record1.iv), record1.encryptedValue)!!.toString(Charsets.UTF_8)
        )
        assertEquals(
            "value2",
            storage.decrypt(getDecryptCipher(record2.iv), record2.encryptedValue)!!.toString(Charsets.UTF_8)
        )
    }

    @Test
    fun `deleting one key does not affect another`() {
        storage.set(getEncryptCipher(), "key1", "value1".toByteArray())
        storage.set(getEncryptCipher(), "key2", "value2".toByteArray())
        storage.delete("key1")

        assertFalse(storage.has("key1"))
        assertTrue(storage.has("key2"))
    }

    @Test
    fun `empty byte array can be stored and retrieved`() {
        storage.set(getEncryptCipher(), "key", ByteArray(0))

        val record = storage.getRecord("key")!!
        val result = storage.decrypt(getDecryptCipher(record.iv), record.encryptedValue)

        assertNotNull(result)
        assertEquals(0, result!!.size)
    }

    @Test
    fun `large value can be stored and retrieved`() {
        val value = ByteArray(1_000_000) { it.toByte() }

        storage.set(getEncryptCipher(), "key", value)

        val record = storage.getRecord("key")!!
        val result = storage.decrypt(getDecryptCipher(record.iv), record.encryptedValue)

        assertArrayEquals(value, result)
    }

    @Test
    fun `keys with special characters are stored safely`() {
        val key = "/\u0000 \t\n*?[]{}()<>|&;~\$`!#\\\"'%"

        storage.set(getEncryptCipher(), key, "value".toByteArray())

        assertTrue(storage.has(key))

        val record = storage.getRecord(key)!!
        val result = storage.decrypt(getDecryptCipher(record.iv), record.encryptedValue)

        assertEquals("value", result!!.toString(Charsets.UTF_8))
    }

    @Test
    fun `two encryptions of the same value produce different ciphertexts`() {
        storage.set(getEncryptCipher(), "key1", "value".toByteArray())
        storage.set(getEncryptCipher(), "key2", "value".toByteArray())

        val record1 = storage.getRecord("key1")!!
        val record2 = storage.getRecord("key2")!!

        assertFalse(record1.encryptedValue.contentEquals(record2.encryptedValue))
    }
}
