package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.CurriculumAiRepository
import com.example.data.ai.GenerationRequest
import com.example.data.backup.BackupBundle
import com.example.data.backup.BackupJsonSerializer
import com.example.data.backup.LocalBackupEntry
import com.example.data.backup.LocalBackupStore
import com.example.data.model.BackupLog
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.DeepLearningCatalog
import com.example.data.model.DocType
import com.example.data.model.TeacherProfile
import com.example.data.model.TeachingDocument
import com.example.data.repository.CurriculumRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiGenState {
    data object Idle : AiGenState
    data object Loading : AiGenState
    data class Streaming(val text: String) : AiGenState
    data class Success(val text: String, val savedDocId: String? = null) : AiGenState
    data class Error(val message: String) : AiGenState
}

data class GeneratorFormState(
    val docType: DocType = DocType.MODUL_AJAR,
    val selectedPresetIndex: Int = 4, // Default: SMP/MTs Fase D (Kelas 7-9)
    val grade: String = "Kelas 7 / Semester 1",
    val subject: String = "IPA",
    val topic: String = "Ekosistem, Interaksi Makhluk Hidup, dan Pelestarian Lingkungan",
    val duration: String = "2 x 40 Menit (1 Pertemuan)",
    val pedagogicalModel: String = DeepLearningCatalog.pedagogicalModels.first(),
    val selectedPrinciples: List<String> = DeepLearningCatalog.principles,
    val selectedDimensions: List<String> = listOf(
        "Penalaran Kritis",
        "Kreativitas",
        "Kolaborasi",
        "Komunikasi"
    ),
    val localContextNotes: String = "",
    val includeSchoolHeader: Boolean = true
)

class MainViewModel(
    private val repository: CurriculumRepository,
    private val aiRepository: CurriculumAiRepository,
    private val localBackupStore: LocalBackupStore,
    private val currentUserId: String
) : ViewModel() {

    val docsState: StateFlow<UiState<List<TeachingDocument>>> =
        repository.observeTeachingDocs(currentUserId)
            .map<List<TeachingDocument>, UiState<List<TeachingDocument>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing teaching docs", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat daftar perangkat ajar."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val profileState: StateFlow<UiState<TeacherProfile>> =
        repository.observeTeacherProfile(currentUserId)
            .map<TeacherProfile?, UiState<TeacherProfile>> { saved ->
                UiState.Success(saved ?: TeacherProfile.sampleDefault(currentUserId))
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing teacher profile", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat profil guru."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val backupLogsState: StateFlow<UiState<List<BackupLog>>> =
        repository.observeBackupLogs(currentUserId)
            .map<List<BackupLog>, UiState<List<BackupLog>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing backup logs", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat riwayat backup."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val cloudBackupsState: StateFlow<UiState<List<CloudBackupSnapshot>>> =
        repository.observeCloudBackups(currentUserId)
            .map<List<CloudBackupSnapshot>, UiState<List<CloudBackupSnapshot>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing cloud backups", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat snapshot cloud."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _localBackups = MutableStateFlow<List<LocalBackupEntry>>(emptyList())
    val localBackups: StateFlow<List<LocalBackupEntry>> = _localBackups.asStateFlow()

    private val _formState = MutableStateFlow(GeneratorFormState())
    val formState: StateFlow<GeneratorFormState> = _formState.asStateFlow()

    private val _aiState = MutableStateFlow<AiGenState>(AiGenState.Idle)
    val aiState: StateFlow<AiGenState> = _aiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterDocType = MutableStateFlow<DocType?>(null)
    val filterDocType: StateFlow<DocType?> = _filterDocType.asStateFlow()

    private val _filterStarredOnly = MutableStateFlow(false)
    val filterStarredOnly: StateFlow<Boolean> = _filterStarredOnly.asStateFlow()

    private val _pendingRestoreBundle = MutableStateFlow<BackupBundle?>(null)
    val pendingRestoreBundle: StateFlow<BackupBundle?> = _pendingRestoreBundle.asStateFlow()

    private val _isBusyBackup = MutableStateFlow(false)
    val isBusyBackup: StateFlow<Boolean> = _isBusyBackup.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var generationJob: Job? = null

    init {
        refreshLocalBackups()
    }

    fun selectDocTypeForGeneration(docType: DocType) {
        _formState.update { it.copy(docType = docType) }
    }

    fun updateForm(transform: (GeneratorFormState) -> GeneratorFormState) {
        _formState.update(transform)
    }

    fun togglePrinciple(principle: String) {
        _formState.update { current ->
            val updated = if (principle in current.selectedPrinciples) {
                if (current.selectedPrinciples.size > 1) current.selectedPrinciples - principle
                else current.selectedPrinciples
            } else {
                current.selectedPrinciples + principle
            }
            current.copy(selectedPrinciples = updated)
        }
    }

    fun toggleDimension(dimension: String) {
        _formState.update { current ->
            val updated = if (dimension in current.selectedDimensions) {
                if (current.selectedDimensions.size > 1) current.selectedDimensions - dimension
                else current.selectedDimensions
            } else {
                (current.selectedDimensions + dimension).take(8)
            }
            current.copy(selectedDimensions = updated)
        }
    }

    fun generateDocument() {
        val form = _formState.value
        if (form.subject.isBlank() || form.topic.isBlank()) {
            _aiState.value = AiGenState.Error("Mata pelajaran dan topik materi wajib diisi sebelum membuat perangkat.")
            return
        }

        val preset = DeepLearningCatalog.phasePresets.getOrElse(form.selectedPresetIndex) {
            DeepLearningCatalog.phasePresets[3]
        }
        val activeProfile = (profileState.value as? UiState.Success)?.data
            ?: TeacherProfile.sampleDefault(currentUserId)

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _aiState.value = AiGenState.Loading
            val accumulated = StringBuilder()
            try {
                val request = GenerationRequest(
                    docType = form.docType,
                    educationLevel = preset.level,
                    phase = preset.phase,
                    grade = form.grade.ifBlank { preset.sampleGrades.first() },
                    subject = form.subject.trim(),
                    topic = form.topic.trim(),
                    duration = form.duration.ifBlank { "2 x 35 Menit" },
                    pedagogicalModel = form.pedagogicalModel,
                    deepLearningPrinciples = form.selectedPrinciples,
                    graduateDimensions = form.selectedDimensions,
                    localContextNotes = form.localContextNotes,
                    includeSchoolHeader = form.includeSchoolHeader,
                    profile = activeProfile
                )
                withTimeout(60_000L) {
                    aiRepository.generateDocumentStream(request).collect { chunk ->
                        accumulated.append(chunk)
                        _aiState.value = AiGenState.Streaming(accumulated.toString())
                    }
                }
                if (accumulated.isNotBlank()) {
                    _aiState.value = AiGenState.Success(accumulated.toString())
                } else {
                    _aiState.value = AiGenState.Error("Model AI tidak mengembalikan konten. Silakan coba lagi.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI generation timed out", e)
                if (accumulated.isNotBlank()) {
                    _aiState.value = AiGenState.Success(accumulated.toString())
                    _messages.tryEmit("Waktu respons melebihi 60 detik; hasil parsial ditampilkan.")
                } else {
                    _aiState.value = AiGenState.Error("Waktu pembuatan melebihi 60 detik. Periksa koneksi lalu coba lagi.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI generation failed", e)
                _aiState.value = AiGenState.Error(
                    e.localizedMessage ?: "Gagal menghasilkan perangkat ajar melalui Firebase AI Logic."
                )
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        if (_aiState.value is AiGenState.Loading) {
            _aiState.value = AiGenState.Idle
        } else if (_aiState.value is AiGenState.Streaming) {
            val partial = (_aiState.value as AiGenState.Streaming).text
            _aiState.value = if (partial.isNotBlank()) AiGenState.Success(partial) else AiGenState.Idle
        }
    }

    fun saveGeneratedDocument(onSaved: (String) -> Unit = {}) {
        val successState = _aiState.value as? AiGenState.Success ?: return
        if (successState.savedDocId != null) {
            _messages.tryEmit("Perangkat sudah tersimpan di Koleksi.")
            return
        }
        val form = _formState.value
        val preset = DeepLearningCatalog.phasePresets.getOrElse(form.selectedPresetIndex) {
            DeepLearningCatalog.phasePresets[3]
        }
        val newDoc = TeachingDocument(
            userId = currentUserId,
            docType = form.docType.name,
            title = "${form.docType.shortLabel}: ${form.topic.trim()}".take(200),
            educationLevel = preset.level,
            phase = preset.phase,
            grade = form.grade.ifBlank { preset.sampleGrades.first() },
            subject = form.subject.trim(),
            topic = form.topic.trim(),
            duration = form.duration,
            pedagogicalModel = form.pedagogicalModel,
            deepLearningPrinciples = form.selectedPrinciples,
            graduateDimensions = form.selectedDimensions,
            contentMarkdown = successState.text,
            starred = false
        )

        viewModelScope.launch {
            repository.createTeachingDoc(currentUserId, newDoc)
                .onSuccess { docId ->
                    _aiState.value = successState.copy(savedDocId = docId)
                    _messages.tryEmit("Berhasil disimpan ke Koleksi & Cloud Firestore.")
                    onSaved(docId)
                }
                .onFailure { err ->
                    _messages.tryEmit("Gagal menyimpan: ${err.localizedMessage ?: "Kesalahan Firestore"}")
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilterDocType(docType: DocType?) {
        _filterDocType.value = docType
    }

    fun toggleFilterStarredOnly() {
        _filterStarredOnly.update { !it }
    }

    fun toggleStarred(doc: TeachingDocument) {
        viewModelScope.launch {
            repository.toggleStarred(currentUserId, doc.id, !doc.starred)
                .onFailure { err ->
                    _messages.tryEmit("Gagal mengubah favorit: ${err.localizedMessage ?: "Kesalahan"}")
                }
        }
    }

    fun updateDocument(docId: String, title: String, topic: String, contentMarkdown: String) {
        viewModelScope.launch {
            repository.updateTeachingDoc(currentUserId, docId, title, topic, contentMarkdown)
                .onSuccess { _messages.tryEmit("Perubahan perangkat ajar disimpan.") }
                .onFailure { err ->
                    _messages.tryEmit("Gagal memperbarui: ${err.localizedMessage ?: "Kesalahan"}")
                }
        }
    }

    fun deleteDocument(docId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTeachingDoc(currentUserId, docId)
                .onSuccess {
                    _messages.tryEmit("Perangkat ajar dihapus.")
                    onDeleted()
                }
                .onFailure { err ->
                    _messages.tryEmit("Gagal menghapus: ${err.localizedMessage ?: "Kesalahan"}")
                }
        }
    }

    fun saveTeacherProfile(profile: TeacherProfile) {
        viewModelScope.launch {
            repository.saveTeacherProfile(currentUserId, profile.copy(userId = currentUserId))
                .onSuccess { _messages.tryEmit("Profil guru & pengesahan berhasil disimpan ke Cloud.") }
                .onFailure { err ->
                    _messages.tryEmit("Gagal menyimpan profil: ${err.localizedMessage ?: "Kesalahan"}")
                }
        }
    }

    fun buildBackupJson(): Pair<String, Int> {
        val profile = (profileState.value as? UiState.Success)?.data
            ?: TeacherProfile.sampleDefault(currentUserId)
        val docs = (docsState.value as? UiState.Success)?.data ?: emptyList()
        val json = BackupJsonSerializer.serialize(profile, docs)
        return json to docs.size
    }

    fun logExportSuccess(itemCount: Int) {
        viewModelScope.launch {
            repository.recordBackupLog(
                userId = currentUserId,
                actionType = "EXPORT_JSON",
                itemCount = itemCount,
                note = "Backup lokal JSON ($itemCount perangkat ajar)"
            )
            _messages.tryEmit("Backup JSON berhasil diekspor ($itemCount perangkat).")
        }
    }

    fun triggerCloudSyncCheck() {
        val count = (docsState.value as? UiState.Success)?.data?.size ?: 0
        viewModelScope.launch {
            _isBusyBackup.value = true
            repository.recordBackupLog(
                userId = currentUserId,
                actionType = "CLOUD_SYNC",
                itemCount = count,
                note = "Verifikasi sinkronisasi Cloud Firestore ($count perangkat)"
            ).onSuccess {
                _messages.tryEmit("Sinkronisasi Cloud Firestore aktif ($count perangkat tersimpan).")
            }.onFailure { err ->
                _messages.tryEmit("Gagal sinkronisasi: ${err.localizedMessage ?: "Kesalahan"}")
            }
            _isBusyBackup.value = false
        }
    }

    fun refreshLocalBackups() {
        viewModelScope.launch {
            _localBackups.value = localBackupStore.listSnapshots(currentUserId)
        }
    }

    fun createLocalBackup(label: String) {
        viewModelScope.launch {
            _isBusyBackup.value = true
            val (json, count) = buildBackupJson()
            val safeLabel = label.trim().ifBlank { "Cadangan Lokal ($count dok)" }
            localBackupStore.saveSnapshot(currentUserId, safeLabel, count, json)
                .onSuccess {
                    refreshLocalBackups()
                    repository.recordBackupLog(
                        userId = currentUserId,
                        actionType = "LOCAL_BACKUP",
                        itemCount = count,
                        note = "Backup lokal: $safeLabel ($count perangkat)"
                    )
                    _messages.tryEmit("Backup lokal berhasil disimpan ($count perangkat).")
                }
                .onFailure { err ->
                    _messages.tryEmit("Gagal menyimpan backup lokal: ${err.localizedMessage ?: "Kesalahan I/O"}")
                }
            _isBusyBackup.value = false
        }
    }

    fun previewLocalBackupRestore(fileName: String) {
        viewModelScope.launch {
            localBackupStore.readSnapshot(currentUserId, fileName)
                .onSuccess { rawJson -> parseRestoreJson(rawJson) }
                .onFailure { err ->
                    _messages.tryEmit(err.localizedMessage ?: "Gagal membaca backup lokal.")
                }
        }
    }

    fun deleteLocalBackup(fileName: String) {
        viewModelScope.launch {
            localBackupStore.deleteSnapshot(currentUserId, fileName)
                .onSuccess {
                    refreshLocalBackups()
                    _messages.tryEmit("Backup lokal dihapus.")
                }
                .onFailure { err ->
                    _messages.tryEmit("Gagal menghapus backup lokal: ${err.localizedMessage ?: "Kesalahan"}")
                }
        }
    }

    fun createCloudBackup(snapshotName: String) {
        viewModelScope.launch {
            _isBusyBackup.value = true
            val (json, count) = buildBackupJson()
            val label = snapshotName.trim().ifBlank { "Snapshot Cloud ($count dok)" }
            repository.createCloudBackup(currentUserId, label, json, count)
                .onSuccess {
                    _messages.tryEmit("Snapshot Cloud berhasil disimpan ($count perangkat).")
                }
                .onFailure { err ->
                    _messages.tryEmit("Gagal membuat snapshot cloud: ${err.localizedMessage ?: "Kesalahan"}")
                }
            _isBusyBackup.value = false
        }
    }

    fun previewCloudBackupRestore(snapshot: CloudBackupSnapshot) {
        parseRestoreJson(snapshot.payloadJson)
    }

    fun deleteCloudBackup(backupId: String) {
        viewModelScope.launch {
            repository.deleteCloudBackup(currentUserId, backupId)
                .onSuccess { _messages.tryEmit("Snapshot Cloud dihapus.") }
                .onFailure { err ->
                    _messages.tryEmit("Gagal menghapus snapshot cloud: ${err.localizedMessage ?: "Kesalahan"}")
                }
        }
    }

    fun parseRestoreJson(rawJson: String) {
        BackupJsonSerializer.deserialize(rawJson)
            .onSuccess { bundle ->
                _pendingRestoreBundle.value = bundle
            }
            .onFailure { err ->
                _messages.tryEmit(err.localizedMessage ?: "File backup tidak valid.")
            }
    }

    fun dismissRestorePreview() {
        _pendingRestoreBundle.value = null
    }

    fun confirmRestore(replaceExisting: Boolean) {
        val bundle = _pendingRestoreBundle.value ?: return
        viewModelScope.launch {
            _isBusyBackup.value = true
            repository.restoreFromBackup(currentUserId, bundle, replaceExisting)
                .onSuccess { count ->
                    _pendingRestoreBundle.value = null
                    _messages.tryEmit("Restore berhasil memulihkan $count perangkat ajar ke Cloud.")
                }
                .onFailure { err ->
                    _messages.tryEmit("Gagal restore: ${err.localizedMessage ?: "Kesalahan"}")
                }
            _isBusyBackup.value = false
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
