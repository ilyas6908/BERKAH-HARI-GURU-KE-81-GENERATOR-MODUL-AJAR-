package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.backup.BackupBundle
import com.example.data.model.BackupLog
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.DocType
import com.example.data.model.TeacherProfile
import com.example.data.model.TeachingDocument
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CurriculumRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireVerifiedUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("Pengguna harus masuk dengan Google sebelum mengakses data.")
        check(uid == expectedUserId) { "Sesi pengguna berubah; menolak akses lintas pengguna." }
        return uid
    }

    fun observeTeachingDocs(userId: String): Flow<List<TeachingDocument>> = flow {
        require(userId.isNotBlank()) { "User ID tidak boleh kosong." }
        val path = "users/$userId/teaching_docs"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("teaching_docs")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TeachingDocument::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTeacherProfile(userId: String): Flow<TeacherProfile?> = flow {
        require(userId.isNotBlank()) { "User ID tidak boleh kosong." }
        val path = "users/$userId/profile/teacher"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("profile")
                .document("teacher")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            TeacherProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeBackupLogs(userId: String): Flow<List<BackupLog>> = flow {
        require(userId.isNotBlank()) { "User ID tidak boleh kosong." }
        val path = "users/$userId/backup_logs"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("backup_logs")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        BackupLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveTeacherProfile(userId: String, profile: TeacherProfile): Result<Unit> {
        val docPath = "users/$userId/profile/teacher"
        return try {
            val uid = requireVerifiedUserId(userId)
            val cleanTeacherName = profile.teacherName.trim().ifBlank { TeacherProfile.DEFAULT_TEACHER_NAME }.take(120)
            val cleanNip = profile.nip.trim().take(40)
            val cleanSchool = profile.schoolName.trim().take(150)
            val cleanNpsn = profile.schoolNpsn.trim().take(30)
            val cleanAddress = profile.schoolAddress.trim().take(250)
            val cleanLevel = profile.educationLevel.trim().take(50)
            val cleanPhase = profile.defaultPhase.trim().take(50)
            val cleanSubject = profile.defaultSubject.trim().take(100)
            val cleanYear = profile.academicYear.trim().take(40)
            val cleanPrincipalName = profile.principalName.trim().take(120)
            val cleanPrincipalNip = profile.principalNip.trim().take(40)

            val docRef = db.collection("users").document(uid).collection("profile").document("teacher")
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "teacherName" to cleanTeacherName,
                        "nip" to cleanNip,
                        "schoolName" to cleanSchool,
                        "schoolNpsn" to cleanNpsn,
                        "schoolAddress" to cleanAddress,
                        "educationLevel" to cleanLevel,
                        "defaultPhase" to cleanPhase,
                        "defaultSubject" to cleanSubject,
                        "academicYear" to cleanYear,
                        "principalName" to cleanPrincipalName,
                        "principalNip" to cleanPrincipalNip,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "teacherName" to cleanTeacherName,
                        "nip" to cleanNip,
                        "schoolName" to cleanSchool,
                        "schoolNpsn" to cleanNpsn,
                        "schoolAddress" to cleanAddress,
                        "educationLevel" to cleanLevel,
                        "defaultPhase" to cleanPhase,
                        "defaultSubject" to cleanSubject,
                        "academicYear" to cleanYear,
                        "principalName" to cleanPrincipalName,
                        "principalNip" to cleanPrincipalNip,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createTeachingDoc(userId: String, doc: TeachingDocument): Result<String> {
        var docPath = "users/$userId/teaching_docs"
        return try {
            val uid = requireVerifiedUserId(userId)
            val payload = validateAndBuildCreatePayload(uid, doc)
            val docRef = db.collection("users").document(uid).collection("teaching_docs").document()
            docPath = docRef.path
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateTeachingDoc(
        userId: String,
        docId: String,
        title: String,
        topic: String,
        contentMarkdown: String
    ): Result<Unit> {
        val docPath = "users/$userId/teaching_docs/$docId"
        return try {
            val uid = requireVerifiedUserId(userId)
            require(docId.isNotBlank() && docId.matches(ID_REGEX)) { "ID dokumen tidak valid." }
            val cleanTitle = title.trim()
            val cleanTopic = topic.trim()
            val cleanContent = contentMarkdown.trim()
            require(cleanTitle.isNotEmpty() && cleanTitle.length <= 200) { "Judul harus 1-200 karakter." }
            require(cleanTopic.isNotEmpty() && cleanTopic.length <= 200) { "Topik harus 1-200 karakter." }
            require(cleanContent.isNotEmpty() && cleanContent.length <= 100000) { "Konten harus 1-100.000 karakter." }

            db.collection("users")
                .document(uid)
                .collection("teaching_docs")
                .document(docId)
                .update(
                    mapOf(
                        "title" to cleanTitle,
                        "topic" to cleanTopic,
                        "contentMarkdown" to cleanContent,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleStarred(userId: String, docId: String, starred: Boolean): Result<Unit> {
        val docPath = "users/$userId/teaching_docs/$docId"
        return try {
            val uid = requireVerifiedUserId(userId)
            require(docId.isNotBlank() && docId.matches(ID_REGEX)) { "ID dokumen tidak valid." }
            db.collection("users")
                .document(uid)
                .collection("teaching_docs")
                .document(docId)
                .update(
                    mapOf(
                        "starred" to starred,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteTeachingDoc(userId: String, docId: String): Result<Unit> {
        val docPath = "users/$userId/teaching_docs/$docId"
        return try {
            val uid = requireVerifiedUserId(userId)
            require(docId.isNotBlank() && docId.matches(ID_REGEX)) { "ID dokumen tidak valid." }
            db.collection("users")
                .document(uid)
                .collection("teaching_docs")
                .document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeCloudBackups(userId: String): Flow<List<CloudBackupSnapshot>> = flow {
        require(userId.isNotBlank()) { "User ID tidak boleh kosong." }
        val path = "users/$userId/cloud_backups"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("cloud_backups")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudBackupSnapshot::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createCloudBackup(
        userId: String,
        snapshotName: String,
        payloadJson: String,
        itemCount: Int
    ): Result<String> {
        var docPath = "users/$userId/cloud_backups"
        return try {
            val uid = requireVerifiedUserId(userId)
            val cleanName = snapshotName.trim().ifBlank { "Cadangan Cloud" }.take(120)
            require(payloadJson.length in 2..800_000) {
                "Ukuran payload backup melebihi batas dokumen cloud (800 KB). Gunakan Ekspor File JSON."
            }
            val safeCount = itemCount.coerceIn(0, 10000)
            val docRef = db.collection("users").document(uid).collection("cloud_backups").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "snapshotName" to cleanName,
                    "itemCount" to safeCount,
                    "payloadJson" to payloadJson,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            recordBackupLog(
                userId = uid,
                actionType = "CLOUD_BACKUP",
                itemCount = safeCount,
                note = "Backup Cloud: $cleanName ($safeCount perangkat)"
            )
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudBackup(userId: String, backupId: String): Result<Unit> {
        val docPath = "users/$userId/cloud_backups/$backupId"
        return try {
            val uid = requireVerifiedUserId(userId)
            require(backupId.isNotBlank() && backupId.matches(ID_REGEX)) { "ID backup tidak valid." }
            db.collection("users")
                .document(uid)
                .collection("cloud_backups")
                .document(backupId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordBackupLog(
        userId: String,
        actionType: String,
        itemCount: Int,
        note: String
    ): Result<Unit> {
        val path = "users/$userId/backup_logs"
        return try {
            val uid = requireVerifiedUserId(userId)
            val allowed = setOf("EXPORT_JSON", "IMPORT_JSON", "CLOUD_SYNC", "LOCAL_BACKUP", "CLOUD_BACKUP")
            val safeAction = if (actionType in allowed) actionType else "CLOUD_SYNC"
            val safeCount = itemCount.coerceIn(0, 10000)
            val safeNote = note.trim().take(200)
            db.collection("users")
                .document(uid)
                .collection("backup_logs")
                .document()
                .set(
                    mapOf(
                        "userId" to uid,
                        "actionType" to safeAction,
                        "itemCount" to safeCount,
                        "note" to safeNote,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun restoreFromBackup(
        userId: String,
        bundle: BackupBundle,
        replaceExisting: Boolean
    ): Result<Int> {
        val basePath = "users/$userId/teaching_docs"
        return try {
            val uid = requireVerifiedUserId(userId)
            val collectionRef = db.collection("users").document(uid).collection("teaching_docs")

            if (replaceExisting) {
                val existingSnapshot = collectionRef.get().await()
                for (chunk in existingSnapshot.documents.chunked(400)) {
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }
            }

            if (bundle.profile != null) {
                saveTeacherProfile(uid, bundle.profile.copy(userId = uid)).getOrThrow()
            }

            var restoredCount = 0
            for (chunk in bundle.documents.chunked(400)) {
                val batch = db.batch()
                chunk.forEach { item ->
                    val targetId = item.id.takeIf { it.isNotBlank() && it.matches(ID_REGEX) }
                    val docRef = if (targetId != null && !replaceExisting) {
                        collectionRef.document()
                    } else if (targetId != null) {
                        collectionRef.document(targetId)
                    } else {
                        collectionRef.document()
                    }
                    val payload = validateAndBuildCreatePayload(uid, item)
                    batch.set(docRef, payload)
                    restoredCount++
                }
                batch.commit().await()
            }

            recordBackupLog(
                userId = uid,
                actionType = "IMPORT_JSON",
                itemCount = restoredCount,
                note = if (replaceExisting) {
                    "Restore (timpa koleksi) $restoredCount perangkat ajar"
                } else {
                    "Restore (gabung) $restoredCount perangkat ajar"
                }
            )
            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    private fun validateAndBuildCreatePayload(
        uid: String,
        doc: TeachingDocument
    ): Map<String, Any> {
        val docType = DocType.fromRaw(doc.docType).name
        val title = doc.title.trim().ifBlank { "Perangkat Mengajar" }.take(200)
        val level = doc.educationLevel.trim().ifBlank { "SD/MI" }.take(50)
        val phase = doc.phase.trim().ifBlank { "Fase C" }.take(50)
        val grade = doc.grade.trim().take(40)
        val subject = doc.subject.trim().ifBlank { "Umum" }.take(100)
        val topic = doc.topic.trim().ifBlank { title }.take(200)
        val duration = doc.duration.trim().take(60)
        val model = doc.pedagogicalModel.trim().take(80)
        val principles = doc.deepLearningPrinciples.map { it.trim() }.filter { it.isNotEmpty() }.take(5)
        val dimensions = doc.graduateDimensions.map { it.trim() }.filter { it.isNotEmpty() }.take(8)
        val content = doc.contentMarkdown.trim().ifBlank { "-" }.take(100000)

        return mapOf(
            "userId" to uid,
            "docType" to docType,
            "title" to title,
            "educationLevel" to level,
            "phase" to phase,
            "grade" to grade,
            "subject" to subject,
            "topic" to topic,
            "duration" to duration,
            "pedagogicalModel" to model,
            "deepLearningPrinciples" to principles,
            "graduateDimensions" to dimensions,
            "contentMarkdown" to content,
            "starred" to doc.starred,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    companion object {
        private val ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
