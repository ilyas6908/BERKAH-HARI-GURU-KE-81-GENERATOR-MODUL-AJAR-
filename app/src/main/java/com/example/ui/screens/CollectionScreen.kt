package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocType
import com.example.data.model.TeachingDocument
import com.example.ui.components.DocTypeBadge
import com.example.ui.components.DocumentExporter
import com.example.ui.components.MarkdownDocumentView
import com.example.ui.components.PoweredByFooter
import com.example.ui.components.PoweredByHeaderBanner
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CollectionScreen(
    docsState: UiState<List<TeachingDocument>>,
    searchQuery: String,
    filterDocType: DocType?,
    filterStarredOnly: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterDocTypeChange: (DocType?) -> Unit,
    onToggleStarredFilter: () -> Unit,
    onToggleStarred: (TeachingDocument) -> Unit,
    onUpdateDoc: (String, String, String, String) -> Unit,
    onDeleteDoc: (String, () -> Unit) -> Unit,
    onNavigateToGenerator: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedDocId by remember { mutableStateOf<String?>(null) }

    val docsList = (docsState as? UiState.Success)?.data ?: emptyList()
    val selectedDoc = docsList.firstOrNull { it.id == selectedDocId }

    if (selectedDoc != null) {
        DocumentDetailScreen(
            doc = selectedDoc,
            onBack = { selectedDocId = null },
            onToggleStarred = { onToggleStarred(selectedDoc) },
            onUpdateDoc = onUpdateDoc,
            onDeleteDoc = {
                onDeleteDoc(selectedDoc.id) { selectedDocId = null }
            },
            modifier = modifier
        )
        return
    }

    val filteredDocs = remember(docsList, searchQuery, filterDocType, filterStarredOnly) {
        docsList.filter { doc ->
            val matchesQuery = searchQuery.isBlank() ||
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.subject.contains(searchQuery, ignoreCase = true) ||
                doc.topic.contains(searchQuery, ignoreCase = true)
            val matchesType = filterDocType == null || doc.safeDocType() == filterDocType
            val matchesStar = !filterStarredOnly || doc.starred
            matchesQuery && matchesType && matchesStar
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                PoweredByHeaderBanner(
                    subtitle = "Koleksi Perangkat Mengajar (${docsList.size} dokumen)"
                )
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Cari") },
                    placeholder = { Text("Cari judul, mata pelajaran, atau topik...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_search_input")
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = filterDocType == null && !filterStarredOnly,
                            onClick = {
                                onFilterDocTypeChange(null)
                                if (filterStarredOnly) onToggleStarredFilter()
                            },
                            label = { Text("Semua") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filterStarredOnly,
                            onClick = onToggleStarredFilter,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Favorit") }
                        )
                    }
                    items(DocType.entries) { type ->
                        FilterChip(
                            selected = filterDocType == type,
                            onClick = {
                                onFilterDocTypeChange(if (filterDocType == type) null else type)
                            },
                            label = { Text(type.shortLabel) }
                        )
                    }
                }
            }

            when (docsState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = docsState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                is UiState.Success -> {
                    if (filteredDocs.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = "Koleksi kosong",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (docsList.isEmpty()) {
                                            "Belum Ada Perangkat Mengajar"
                                        } else {
                                            "Tidak Ada Hasil Pencarian"
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (docsList.isEmpty()) {
                                            "Buat Modul Ajar, ATP, LKPD, atau Rubrik Asesmen baru melalui menu Generator, atau pulihkan dari Backup JSON."
                                        } else {
                                            "Coba ubah kata kunci pencarian atau pilih filter jenis perangkat lainnya."
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (docsList.isEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = onNavigateToGenerator,
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("empty_collection_generate_btn")
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Buat Perangkat Baru")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredDocs, key = { it.id }) { doc ->
                            TeachingDocListCard(
                                doc = doc,
                                onClick = { selectedDocId = doc.id },
                                onToggleStar = { onToggleStarred(doc) }
                            )
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
private fun TeachingDocListCard(
    doc: TeachingDocument,
    onClick: () -> Unit,
    onToggleStar: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")) }
    val dateLabel = (doc.updatedAt ?: doc.createdAt)?.toDate()?.let { dateFormat.format(it) } ?: "Baru saja"
    val docType = doc.safeDocType()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doc_card_${doc.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DocTypeBadge(text = docType.badgeText)
                    Text(
                        text = "${doc.educationLevel} • ${doc.phase}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${doc.subject} • ${doc.topic}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Diperbarui: $dateLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            IconButton(
                onClick = onToggleStar,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("star_btn_${doc.id}")
            ) {
                Icon(
                    imageVector = if (doc.starred) Icons.Filled.Star else Icons.Filled.StarOutline,
                    contentDescription = if (doc.starred) "Hapus dari favorit" else "Tandai favorit",
                    tint = if (doc.starred) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocumentDetailScreen(
    doc: TeachingDocument,
    onBack: () -> Unit,
    onToggleStarred: () -> Unit,
    onUpdateDoc: (String, String, String, String) -> Unit,
    onDeleteDoc: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        var editTitle by remember(doc.id) { mutableStateOf(doc.title) }
        var editTopic by remember(doc.id) { mutableStateOf(doc.topic) }
        var editContent by remember(doc.id) { mutableStateOf(doc.contentMarkdown) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Perangkat Mengajar") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Judul") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editTopic,
                        onValueChange = { editTopic = it },
                        label = { Text("Topik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Isi Dokumen (Markdown)") },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDoc(doc.id, editTitle, editTopic, editContent)
                        showEditDialog = false
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Perangkat Mengajar?") },
            text = { Text("Dokumen \"${doc.title}\" akan dihapus permanen dari koleksi cloud Anda.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteDoc()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detail_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                        Column {
                            Text(
                                text = doc.safeDocType().label,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${doc.subject} • ${doc.phase}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row {
                        IconButton(
                            onClick = onToggleStarred,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (doc.starred) Icons.Filled.Star else Icons.Filled.StarOutline,
                                contentDescription = "Favorit",
                                tint = if (doc.starred) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detail_edit_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Dokumen")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("detail_delete_button")
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Hapus Dokumen",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item {
                PoweredByHeaderBanner(
                    subtitle = "${doc.title}"
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            DocumentExporter.printOrSavePdf(context, doc.title, doc.contentMarkdown)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("detail_print_pdf_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cetak / Simpan PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            DocumentExporter.copyToClipboard(context, doc.title, doc.contentMarkdown)
                            Toast.makeText(context, "Konten disalin ke papan klip", Toast.LENGTH_SHORT).show()
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
                            DocumentExporter.shareText(context, doc.title, doc.contentMarkdown)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan")
                    }
                }
            }

            item {
                MarkdownDocumentView(markdown = doc.contentMarkdown)
            }

            item {
                PoweredByFooter()
            }
        }
    }
}
