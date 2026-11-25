package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeepLearningCatalog
import com.example.data.model.DocType
import com.example.data.model.TeacherProfile
import com.example.ui.components.DocumentExporter
import com.example.ui.components.MarkdownDocumentView
import com.example.ui.components.PoweredByFooter
import com.example.ui.components.PoweredByHeaderBanner
import com.example.ui.viewmodel.AiGenState
import com.example.ui.viewmodel.GeneratorFormState
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneratorScreen(
    formState: GeneratorFormState,
    aiState: AiGenState,
    profileState: UiState<TeacherProfile>,
    onUpdateForm: ((GeneratorFormState) -> GeneratorFormState) -> Unit,
    onTogglePrinciple: (String) -> Unit,
    onToggleDimension: (String) -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    onSaveDoc: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data ?: TeacherProfile.sampleDefault()
    val currentPreset = DeepLearningCatalog.phasePresets.getOrElse(formState.selectedPresetIndex) {
        DeepLearningCatalog.phasePresets[3]
    }
    val isGenerating = aiState is AiGenState.Loading || aiState is AiGenState.Streaming

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
                    subtitle = "Studio Generator • ${formState.docType.label}"
                )
            }

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
                        Text(
                            text = "1. Jenis Perangkat Mengajar",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DocType.entries.forEach { type ->
                                FilterChip(
                                    selected = formState.docType == type,
                                    onClick = { onUpdateForm { it.copy(docType = type) } },
                                    label = { Text(type.shortLabel) },
                                    modifier = Modifier.testTag("chip_doctype_${type.name.lowercase()}")
                                )
                            }
                        }

                        Text(
                            text = "2. Jenjang & Fase Kurikulum Merdeka",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DeepLearningCatalog.phasePresets.forEachIndexed { idx, preset ->
                                FilterChip(
                                    selected = formState.selectedPresetIndex == idx,
                                    onClick = {
                                        onUpdateForm {
                                            it.copy(
                                                selectedPresetIndex = idx,
                                                grade = preset.sampleGrades.first()
                                            )
                                        }
                                    },
                                    label = { Text("${preset.level} • ${preset.phase}") }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = formState.grade,
                                onValueChange = { valText -> onUpdateForm { it.copy(grade = valText) } },
                                label = { Text("Kelas / Semester") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_grade")
                            )
                            OutlinedTextField(
                                value = formState.duration,
                                onValueChange = { valText -> onUpdateForm { it.copy(duration = valText) } },
                                label = { Text("Alokasi Waktu (JP)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_duration")
                            )
                        }

                        OutlinedTextField(
                            value = formState.subject,
                            onValueChange = { valText -> onUpdateForm { it.copy(subject = valText) } },
                            label = { Text("Mata Pelajaran") },
                            placeholder = { Text("Contoh: IPAS, Matematika, Bahasa Indonesia") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_subject")
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DeepLearningCatalog.sampleSubjects.take(6).forEach { subj ->
                                FilterChip(
                                    selected = formState.subject.equals(subj, ignoreCase = true),
                                    onClick = { onUpdateForm { it.copy(subject = subj) } },
                                    label = { Text(subj, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = formState.topic,
                            onValueChange = { valText -> onUpdateForm { it.copy(topic = valText) } },
                            label = { Text("Topik / Materi Pokok") },
                            placeholder = { Text("Contoh: Ekosistem, Pecahan Senilai, Teks Eksplanasi") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_topic")
                        )

                        Text(
                            text = "3. Integrasi Deep Learning (Kemendikdasmen)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Prinsip Pembelajaran Mendalam:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DeepLearningCatalog.principles.forEach { p ->
                                FilterChip(
                                    selected = p in formState.selectedPrinciples,
                                    onClick = { onTogglePrinciple(p) },
                                    label = { Text(p) }
                                )
                            }
                        }

                        Text(
                            text = "Model Praktik Pedagogis:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DeepLearningCatalog.pedagogicalModels.forEach { m ->
                                FilterChip(
                                    selected = formState.pedagogicalModel == m,
                                    onClick = { onUpdateForm { it.copy(pedagogicalModel = m) } },
                                    label = { Text(m) }
                                )
                            }
                        }

                        Text(
                            text = "8 Dimensi Profil Lulusan:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DeepLearningCatalog.graduateDimensions.forEach { dim ->
                                FilterChip(
                                    selected = dim in formState.selectedDimensions,
                                    onClick = { onToggleDimension(dim) },
                                    label = { Text(dim, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = formState.localContextNotes,
                            onValueChange = { valText -> onUpdateForm { it.copy(localContextNotes = valText) } },
                            label = { Text("Konteks Sekolah / Kebutuhan Diferensiasi (Opsional)") },
                            placeholder = { Text("Misal: Lingkungan pesisir, proyek kelompok heterogen, media digital") },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_context_notes")
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sertakan Identitas & Pengesahan Sekolah",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "${profile.schoolName} (NPSN ${profile.schoolNpsn}) • ${profile.teacherName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = formState.includeSchoolHeader,
                                    onCheckedChange = { checked ->
                                        onUpdateForm { it.copy(includeSchoolHeader = checked) }
                                    },
                                    modifier = Modifier.testTag("switch_include_header")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onGenerate,
                                enabled = !isGenerating,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("generate_button")
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Menyusun Perangkat...")
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Buat ${formState.docType.shortLabel}")
                                }
                            }

                            if (isGenerating) {
                                OutlinedButton(
                                    onClick = onCancel,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .heightIn(min = 52.dp)
                                        .testTag("cancel_generate_button")
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = "Hentikan")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hentikan")
                                }
                            }
                        }
                    }
                }
            }

            when (aiState) {
                is AiGenState.Idle -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Siap Menyusun ${formState.docType.label}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Target: ${currentPreset.level} (${currentPreset.phase}) • ${formState.subject}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                is AiGenState.Loading -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Menyusun perangkat mengajar Kurikulum Merdeka & Deep Learning...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                is AiGenState.Streaming -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Sedang menulis perangkat ajar secara langsung...",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            MarkdownDocumentView(markdown = aiState.text)
                        }
                    }
                }

                is AiGenState.Success -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hasil Perangkat Mengajar",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (aiState.savedDocId != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = "Tersimpan",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Tersimpan",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onSaveDoc,
                                    enabled = aiState.savedDocId == null,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("save_generated_doc_button")
                                ) {
                                    Icon(Icons.Filled.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (aiState.savedDocId == null) "Simpan ke Koleksi" else "Sudah Tersimpan")
                                }

                                OutlinedButton(
                                    onClick = {
                                        DocumentExporter.printOrSavePdf(
                                            context = context,
                                            title = "${formState.docType.shortLabel} - ${formState.topic}",
                                            content = aiState.text
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("print_pdf_generated_button")
                                ) {
                                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cetak / PDF")
                                }

                                OutlinedButton(
                                    onClick = {
                                        DocumentExporter.copyToClipboard(context, formState.docType.label, aiState.text)
                                        Toast.makeText(context, "Teks disalin ke papan klip", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Salin")
                                }

                                OutlinedButton(
                                    onClick = {
                                        DocumentExporter.shareText(
                                            context,
                                            "${formState.docType.label}: ${formState.topic}",
                                            aiState.text
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bagikan")
                                }
                            }

                            MarkdownDocumentView(markdown = aiState.text)
                        }
                    }
                }

                is AiGenState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Gagal Membuat Perangkat",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = aiState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
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
