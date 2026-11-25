package com.example.data.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocalBackupEntry(
    val fileName: String,
    val displayName: String,
    val itemCount: Int,
    val byteSize: Long,
    val lastModifiedMillis: Long
)

class LocalBackupStore(private val rootFilesDir: File) {

    private fun userDir(userId: String): File {
        val safeUid = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val dir = File(File(rootFilesDir, "ajarmerdeka_backups"), safeUid)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    suspend fun saveSnapshot(
        userId: String,
        label: String,
        itemCount: Int,
        jsonPayload: String
    ): Result<LocalBackupEntry> = withContext(Dispatchers.IO) {
        try {
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val safeLabel = label.trim()
                .ifBlank { "Backup_$stamp" }
                .replace(Regex("[^a-zA-Z0-9_\\- ]"), "")
                .replace(" ", "_")
                .take(40)
            val fileName = "${stamp}__${itemCount}__${safeLabel}.json"
            val target = File(userDir(userId), fileName)
            target.writeText(jsonPayload, Charsets.UTF_8)
            Result.success(
                LocalBackupEntry(
                    fileName = fileName,
                    displayName = label.trim().ifBlank { "Backup Lokal ($stamp)" },
                    itemCount = itemCount,
                    byteSize = target.length(),
                    lastModifiedMillis = target.lastModified()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listSnapshots(userId: String): List<LocalBackupEntry> = withContext(Dispatchers.IO) {
        val dir = userDir(userId)
        val files = dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?: return@withContext emptyList()
        files.sortedByDescending { it.lastModified() }.map { file ->
            val parts = file.nameWithoutExtension.split("__", limit = 3)
            val parsedCount = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val rawLabel = parts.getOrNull(2)?.replace("_", " ") ?: file.nameWithoutExtension
            LocalBackupEntry(
                fileName = file.name,
                displayName = rawLabel,
                itemCount = parsedCount,
                byteSize = file.length(),
                lastModifiedMillis = file.lastModified()
            )
        }
    }

    suspend fun readSnapshot(userId: String, fileName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = File(fileName).name
            val target = File(userDir(userId), sanitizedName)
            if (!target.exists() || !target.isFile) {
                return@withContext Result.failure(IllegalStateException("File backup lokal tidak ditemukan."))
            }
            Result.success(target.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSnapshot(userId: String, fileName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = File(fileName).name
            val target = File(userDir(userId), sanitizedName)
            if (target.exists()) {
                target.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
