package org.racehorse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class EncryptedStoragePluginTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var plugin: EncryptedStoragePlugin

    @Before
    fun setUp() {
        plugin = EncryptedStoragePlugin(
            storageDir = tempFolder.root,
            secretKeyIterationCount = 1024,
            secretKeySize = 256
        )
    }

    class MockSetEvent(key: String, value: String, password: String) : SetEncryptedValueEvent(key, value, password) {
        var lastEvent: ChainableEvent? = null

        override fun respond(event: ChainableEvent) {
            this.lastEvent = event
        }
    }

    class MockGetEvent(key: String, password: String) : GetEncryptedValueEvent(key, password) {
        var lastEvent: ChainableEvent? = null

        override fun respond(event: ChainableEvent) {
            this.lastEvent = event
        }
    }

    class MockHasEvent(key: String) : HasEncryptedValueEvent(key) {
        var lastEvent: ChainableEvent? = null

        override fun respond(event: ChainableEvent) {
            this.lastEvent = event
        }
    }

    class MockDeleteEvent(key: String) : DeleteEncryptedValueEvent(key) {
        var lastEvent: ChainableEvent? = null

        override fun respond(event: ChainableEvent) {
            this.lastEvent = event
        }
    }

    @Test
    fun `set and get are symmetrical`() {
        // Set
        val setEvent = MockSetEvent("key", "value", "password")
        plugin.onSetEncryptedValue(setEvent)
        assertTrue((setEvent.lastEvent as SetEncryptedValueEvent.ResultEvent).isSuccessful)

        // Get
        val getEvent = MockGetEvent("key", "password")
        plugin.onGetEncryptedValue(getEvent)
        assertEquals("value", (getEvent.lastEvent as GetEncryptedValueEvent.ResultEvent).value)
    }

    @Test
    fun `set overwrites existing key`() {
        plugin.onSetEncryptedValue(MockSetEvent("key", "value1", "password1"))
        plugin.onSetEncryptedValue(MockSetEvent("key", "value2", "password2"))

        // Get
        val getEvent = MockGetEvent("key", "password2")
        plugin.onGetEncryptedValue(getEvent)
        assertEquals("value2", (getEvent.lastEvent as GetEncryptedValueEvent.ResultEvent).value)
    }

    @Test
    fun `get returns null for missing key`() {
        val getEvent = MockGetEvent("key", "password")
        plugin.onGetEncryptedValue(getEvent)
        assertNull((getEvent.lastEvent as GetEncryptedValueEvent.ResultEvent).value)
    }

    @Test
    fun `has returns false for a missing key`() {
        val hasEvent = MockHasEvent("key")
        plugin.onHasEncryptedValue(hasEvent)
        assertFalse((hasEvent.lastEvent as HasEncryptedValueEvent.ResultEvent).isExisting)
    }

    @Test
    fun `has returns true after set`() {
        plugin.onSetEncryptedValue(MockSetEvent("key", "value", "password"))

        val hasEvent = MockHasEvent("key")
        plugin.onHasEncryptedValue(hasEvent)
        assertTrue((hasEvent.lastEvent as HasEncryptedValueEvent.ResultEvent).isExisting)
    }

    @Test
    fun `has returns false after delete`() {
        plugin.onSetEncryptedValue(MockSetEvent("key", "value", "password"))

        plugin.onDeleteEncryptedValue(MockDeleteEvent("key"))

        val hasEvent = MockHasEvent("key")
        plugin.onHasEncryptedValue(hasEvent)
        assertFalse((hasEvent.lastEvent as HasEncryptedValueEvent.ResultEvent).isExisting)
    }

    @Test
    fun `get returns null after delete`() {
        plugin.onSetEncryptedValue(MockSetEvent("key", "value", "password"))

        plugin.onDeleteEncryptedValue(MockDeleteEvent("key"))

        val getEvent = MockGetEvent("key", "password")
        plugin.onGetEncryptedValue(getEvent)
        assertNull((getEvent.lastEvent as GetEncryptedValueEvent.ResultEvent).value)
    }

    @Test
    fun `get returns null if password is wrong`() {
        plugin.onSetEncryptedValue(MockSetEvent("key", "value", "password"))

        val getEvent = MockGetEvent("key", "wrongPassword")
        plugin.onGetEncryptedValue(getEvent)
        assertNull((getEvent.lastEvent as GetEncryptedValueEvent.ResultEvent).value)
    }

    @Test
    fun `delete returns true for existing key`() {
        plugin.onSetEncryptedValue(MockSetEvent("key", "value", "password"))

        val deleteEvent = MockDeleteEvent("key")
        plugin.onDeleteEncryptedValue(deleteEvent)
        assertTrue((deleteEvent.lastEvent as DeleteEncryptedValueEvent.ResultEvent).isDeleted)
    }

    @Test
    fun `delete returns false for missing key`() {
        val deleteEvent = MockDeleteEvent("key")
        plugin.onDeleteEncryptedValue(deleteEvent)
        assertFalse((deleteEvent.lastEvent as DeleteEncryptedValueEvent.ResultEvent).isDeleted)
    }
}
