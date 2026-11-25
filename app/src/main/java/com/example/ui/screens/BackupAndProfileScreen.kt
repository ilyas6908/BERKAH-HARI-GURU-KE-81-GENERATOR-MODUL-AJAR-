package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.backup.BackupBundle
import com.example.data.backup.LocalBackupEntry
import com.example.data.model.BackupLog
import com.example.data.model.CloudBackupSnapshot
import com.example.data.model.TeacherProfile
import com.example.data.model.TeachingDocument
import com.example.ui.components.PoweredByFooter
import com.example.ui.components.PoweredByHeaderBanner
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupAndProfileScreen(
    userEmail: String?,
    userName: String?,
    profileState: UiState<TeacherProfile>,
    docsState: UiState<List<TeachingDocument>>,
    backupLogsState: UiState<List<BackupLog>>,
    cloudBackupsState: UiState<List<CloudBackupSnapshot>>,
    localBackups: List<LocalBackupEntry>,
    pendingRestoreBundle: BackupBundle?,
    isBusyBackup: Boolean,
    onSaveProfile: (TeacherProfile) -> Unit,
    onBuildBackupJson: () -> Pair<String, Int>,
    onLogExportSuccess: (Int) -> Unit,
    onCreateLocalBackup: (String) -> Unit,
    onPreviewLocalRestore: (String) -> Unit,
    onDeleteLocalBackup: (String) -> Unit,
    onCreateCloudBackup: (String) -> Unit,
    onPreviewCloudRestore: (CloudBackupSnapshot) -> Unit,
    onDeleteCloudBackup: (String) -> Unit,
    onParseRestoreJson: (String) -> Unit,
    onConfirmRestore: (Boolean) -> Unit,
    onDismissRestore: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val loadedProfile = (profileState as? UiState.Success)?.data ?: TeacherProfile.sampleDefault()
    val docsCount = (docsState as? UiState.Success)?.data?.size ?: 0
    val logs = (backupLogsState as? UiState.Success)?.data ?: emptyList()
    val cloudBackups = (cloudBackupsState as? UiState.Success)?.data ?: emptyList()

    var teacherName by remember { mutableStateOf(loadedProfile.teacherName) }
    var nip by remember { mutableStateOf(loadedProfile.nip) }
    var schoolName by remember { mutableStateOf(loadedProfile.schoolName) }
    var schoolNpsn by remember { mutableStateOf(loadedProfile.schoolNpsn) }
    var schoolAddress by remember { mutableStateOf(loadedProfile.schoolAddress) }
    var academicYear by remember { mutableStateOf(loadedProfile.academicYear) }
    var principalName by remember { mutableStateOf(loadedProfile.principalName) }
    var principalNip by remember { mutableStateOf(loadedProfile.principalNip) }

    var snapshotLabel by remember { mutableStateOf("") }
    var showSchemaDetails by remember { mutableStateOf(false) }

    LaunchedEffect(loadedProfile) {
        teacherName = loadedProfile.teacherName
        nip = loadedProfile.nip
        schoolName = loadedProfile.schoolName
        schoolNpsn = loadedProfile.schoolNpsn
        schoolAddress = loadedProfile.schoolAddress
        academicYear = loadedProfile.academicYear
        principalName = loadedProfile.principalName
        principalNip = loadedProfile.principalNip
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val (json, count) = onBuildBackupJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                onLogExportSuccess(count)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Gagal menulis file backup: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!raw.isNullOrBlank()) {
                    onParseRestoreJson(raw)
                } else {
                    Toast.makeText(context, "File backup kosong.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Gagal membaca file backup: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    if (pendingRestoreBundle != null) {
        RestorePreviewDialog(
            bundle = pendingRestoreBundle,
            isBusy = isBusyBackup,
            onMerge = { onConfirmRestore(false) },
            onReplace = { onConfirmRestore(true) },
            onDismiss = onDismissRestore
        )
    }

    val fmt = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                PoweredByHeaderBanner(
                    subtitle = "Skema Penyimpanan, Backup & Restore • Profil Guru"
                )
            }

            // 1. Data Storage Schema Overview Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSchemaDetails = !showSchemaDetails }
                        .testTag("storage_schema_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = "Skema Penyimpanan Data",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Skema Penyimpanan Data Hibrida (Lokal & Cloud)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "$docsCount perangkat aktif • ${localBackups.size} backup lokal • ${cloudBackups.size} snapshot cloud",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = if (showSchemaDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (showSchemaDetails) "Tutup skema" else "Lihat skema"
                            )
                        }

                        AnimatedVisibility(visible = showSchemaDetails) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                Text(
                                    text = "1. Cloud Firestore (Per Akun Google UID):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "• /users/{uid}/profile/teacher (Identitas Guru & Kepala Sekolah)\n" +
                                        "• /users/{uid}/teaching_docs/{docId} (Modul Ajar, ATP, LKPD, Asesmen, Prosem, P5, Refleksi)\n" +
                                        "• /users/{uid}/cloud_backups/{backupId} (Snapshot JSON Cloud siap restore)\n" +
                                        "• /users/{uid}/backup_logs/{logId} (Log riwayat backup & restore)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "2. Penyimpanan Lokal Perangkat (JSON Vault & SAF):",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "• Internal Vault: filesDir/ajarmerdeka_backups/{uid}/*.json\n" +
                                        "• Ekspor/Impor Eksternal: Dokumen JSON portabel via Android Storage Access Framework (tanpa izin penyimpanan berlebih).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Snapshot Label & Quick Dual Backup Actions
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CloudDone,
                                contentDescription = "Backup Database",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Buat Cadangan Database (Lokal / Cloud)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Simpan seluruh perangkat ajar & profil guru untuk dipulihkan kapan saja",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = snapshotLabel,
                            onValueChange = { snapshotLabel = it },
                            label = { Text("Nama / Label Backup (Opsional)") },
                            placeholder = { Text("Misal: Cadangan Semester Ganjil 2026") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backup_label_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    onCreateCloudBackup(snapshotLabel)
                                    snapshotLabel = ""
                                },
                                enabled = !isBusyBackup,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("create_cloud_backup_button")
                            ) {
                                Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Cloud")
                            }

                            Button(
                                onClick = {
                                    onCreateLocalBackup(snapshotLabel)
                                    snapshotLabel = ""
                                },
                                enabled = !isBusyBackup,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp)
                                    .testTag("create_local_backup_button")
                            ) {
                                Icon(Icons.Filled.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Lokal")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                    exportJsonLauncher.launch("AjarMerdeka_Backup_$stamp.json")
                                },
                                enabled = !isBusyBackup,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("export_backup_json_button")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ekspor .JSON")
                            }

                            OutlinedButton(
                                onClick = {
                                    importJsonLauncher.launch(arrayOf("application/json", "*/*"))
                                },
                                enabled = !isBusyBackup,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("import_restore_json_button")
                            ) {
                                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore File")
                            }
                        }

                        if (isBusyBackup) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Memproses backup / restore...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // 3. Cloud Snapshots List (Restore Anytime)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Daftar Snapshot Cloud (${cloudBackups.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (cloudBackups.isEmpty()) {
                            Text(
                                text = "Belum ada snapshot cloud. Tekan \"Backup Cloud\" di atas untuk menyimpan cadangan ke Firestore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            cloudBackups.forEach { snap ->
                                val timeText = snap.createdAt?.toDate()?.let { fmt.format(it) } ?: "Baru saja"
                                SnapshotRowItem(
                                    title = snap.snapshotName,
                                    subtitle = "${snap.itemCount} perangkat • $timeText",
                                    onRestore = { onPreviewCloudRestore(snap) },
                                    onDelete = { onDeleteCloudBackup(snap.id) },
                                    restoreTag = "restore_cloud_${snap.id}"
                                )
                            }
                        }
                    }
                }
            }

            // 4. Local Device Vault List (Restore Anytime)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Daftar Backup Penyimpanan Lokal (${localBackups.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        if (localBackups.isEmpty()) {
                            Text(
                                text = "Belum ada backup lokal di perangkat. Tekan \"Backup Lokal\" untuk menyimpan salinan offline.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            localBackups.forEach { entry ->
                                val timeText = fmt.format(Date(entry.lastModifiedMillis))
                                val sizeKb = (entry.byteSize / 1024.0).coerceAtLeast(0.1)
                                val sizeText = String.format(Locale.US, "%.1f KB", sizeKb)
                                SnapshotRowItem(
                                    title = entry.displayName,
                                    subtitle = "${entry.itemCount} perangkat • $sizeText • $timeText",
                                    onRestore = { onPreviewLocalRestore(entry.fileName) },
                                    onDelete = { onDeleteLocalBackup(entry.fileName) },
                                    restoreTag = "restore_local_${entry.fileName}"
                                )
                            }
                        }
                    }
                }
            }

            // 5. Activity Logs
            if (logs.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = "Riwayat Backup",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Riwayat Aktivitas Backup & Restore",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            logs.take(6).forEach { log ->
                                val timeText = log.createdAt?.toDate()?.let { fmt.format(it) } ?: "Baru saja"
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = log.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = timeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Teacher Profile & Pengesahan Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Profil Guru",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Profil Guru & Pengesahan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(
                                onClick = {
                                    teacherName = TeacherProfile.DEFAULT_TEACHER_NAME
                                    nip = TeacherProfile.DEFAULT_TEACHER_NIP
                                    schoolName = TeacherProfile.DEFAULT_SCHOOL_NAME
                                    schoolNpsn = TeacherProfile.DEFAULT_SCHOOL_NPSN
                                    schoolAddress = TeacherProfile.DEFAULT_SCHOOL_ADDRESS
                                    principalName = TeacherProfile.DEFAULT_PRINCIPAL_NAME
                                    principalNip = TeacherProfile.DEFAULT_PRINCIPAL_NIP
                                    academicYear = "2026/2027"
                                },
                                modifier = Modifier.testTag("fill_sample_profile_button")
                            ) {
                                Text("Isi Contoh Profil")
                            }
                        }

                        OutlinedTextField(
                            value = teacherName,
                            onValueChange = { teacherName = it },
                            label = { Text("Nama Guru & Gelar") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_teacher_name")
                        )

                        OutlinedTextField(
                            value = nip,
                            onValueChange = { nip = it },
                            label = { Text("NIP Guru") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_teacher_nip")
                        )

                        OutlinedTextField(
                            value = schoolName,
                            onValueChange = { schoolName = it },
                            label = { Text("Satuan Pendidikan / Sekolah") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_school_name")
                        )

                        OutlinedTextField(
                            value = schoolNpsn,
                            onValueChange = { schoolNpsn = it },
                            label = { Text("NPSN Sekolah") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_school_npsn")
                        )

                        OutlinedTextField(
                            value = schoolAddress,
                            onValueChange = { schoolAddress = it },
                            label = { Text("Alamat Sekolah") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_school_address")
                        )

                        OutlinedTextField(
                            value = academicYear,
                            onValueChange = { academicYear = it },
                            label = { Text("Tahun Ajaran") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = principalName,
                            onValueChange = { principalName = it },
                            label = { Text("Nama Kepala Sekolah") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_principal_name")
                        )

                        OutlinedTextField(
                            value = principalNip,
                            onValueChange = { principalNip = it },
                            label = { Text("NIP Kepala Sekolah") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_principal_nip")
                        )

                        Button(
                            onClick = {
                                onSaveProfile(
                                    loadedProfile.copy(
                                        teacherName = teacherName,
                                        nip = nip,
                                        schoolName = schoolName,
                                        schoolNpsn = schoolNpsn,
                                        schoolAddress = schoolAddress,
                                        academicYear = academicYear,
                                        principalName = principalName,
                                        principalNip = principalNip
                                    )
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("save_profile_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan Profil ke Cloud")
                        }
                    }
                }
            }

            // 7. Google Account Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Akun Google Terhubung",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${userName ?: "Guru Indonesia"} (${userEmail ?: "Terautentikasi"})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onSignOut,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("sign_out_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Keluar Akun")
                        }
                    }
                }
            }

            item {
                PoweredByFooter()
            }
        }
    }
}

@Composable
private fun SnapshotRowItem(
    title: String,
    subtitle: String,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    restoreTag: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onRestore,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag(restoreTag)
            ) {
                Icon(Icons.Filled.Restore, contentDescription = "Restore", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore", style = MaterialTheme.typography.labelMedium)
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Hapus Backup",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun RestorePreviewDialog(
    bundle: BackupBundle,
    isBusy: Boolean,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        icon = { Icon(Icons.Filled.Restore, contentDescription = null) },
        title = { Text("Konfirmasi Restore Database") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Data cadangan siap dipulihkan:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "• Jumlah Perangkat Mengajar: ${bundle.documents.size} dokumen",
                    style = MaterialTheme.typography.bodySmall
                )
                if (bundle.profile != null) {
                    Text(
                        text = "• Profil Guru: ${bundle.profile.teacherName} (NIP. ${bundle.profile.nip})",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (bundle.exportedAt.isNotBlank()) {
                    Text(
                        text = "• Waktu Backup: ${bundle.exportedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pilih \"Gabung\" untuk menambahkan ke koleksi saat ini, atau \"Timpa\" untuk mengganti seluruh koleksi lama.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onReplace,
                    enabled = !isBusy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Timpa")
                }
                Button(
                    onClick = onMerge,
                    enabled = !isBusy
                ) {
                    Text("Gabung")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text("Batal")
            }
        }
    )
}
