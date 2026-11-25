package com.example

import com.example.data.backup.LocalBackupStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalBackupStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun saveListReadAndDelete_localBackupSnapshotWorks() = runBlocking {
        val store = LocalBackupStore(tempFolder.root)
        val json = """{"version":1,"documents":[]}"""

        val saved = store.saveSnapshot("user_abc", "Cadangan Harian", 4, json)
        assertTrue(saved.isSuccess)
        val entry = saved.getOrThrow()

        val list = store.listSnapshots("user_abc")
        assertEquals(1, list.size)
        assertEquals(4, list.first().itemCount)
        assertEquals("Cadangan Harian", list.first().displayName)

        val read = store.readSnapshot("user_abc", entry.fileName)
        assertTrue(read.isSuccess)
        assertEquals(json, read.getOrThrow())

        val deleted = store.deleteSnapshot("user_abc", entry.fileName)
        assertTrue(deleted.isSuccess)
        assertTrue(store.listSnapshots("user_abc").isEmpty())
    }
}
