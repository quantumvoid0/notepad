package com.github.quantumvoid0.notepad

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    notes: List<Note>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNoteClick: (String) -> Unit,
    onNewNote: (NoteType) -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String) -> Unit,
    onAbout: () -> Unit,
) {
    var fabExpanded by remember { mutableStateOf(false) }
    var selectedNoteId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            "Notepad",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    actions = {
                        IconButton(onClick = onAbout) {
                            Icon(Icons.Outlined.Info, contentDescription = "About")
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                )
                // Search bar
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = onSearchChange,
                            onSearch = {},
                            expanded = false,
                            onExpandedChange = {},
                            placeholder = { Text("Search notes…") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                        )
                    },
                    expanded = false,
                    onExpandedChange = {},
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                ) {}
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = fabExpanded,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SmallFabItem(
                            icon = Icons.Outlined.CheckBox,
                            label = "Checklist",
                            onClick = {
                                fabExpanded = false
                                onNewNote(NoteType.CHECKLIST)
                            },
                        )
                        SmallFabItem(
                            icon = Icons.Outlined.TextFields,
                            label = "Text note",
                            onClick = {
                                fabExpanded = false
                                onNewNote(NoteType.TEXT)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    AnimatedContent(fabExpanded) { expanded ->
                        if (expanded) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "New note")
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (notes.isEmpty()) {
            EmptyState(modifier = Modifier.padding(padding))
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding =
                    PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = padding.calculateTopPadding() + 8.dp,
                        bottom = padding.calculateBottomPadding() + 80.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        isSelected = selectedNoteId == note.id,
                        onClick = {
                            if (selectedNoteId != null) {
                                selectedNoteId = null
                            } else {
                                onNoteClick(note.id)
                            }
                        },
                        onLongClick = { selectedNoteId = note.id },
                        onDelete = {
                            selectedNoteId = null
                            onDelete(note.id)
                        },
                        onPin = {
                            selectedNoteId = null
                            onPin(note.id)
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
fun SmallFabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.width(8.dp))
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ) {
            Icon(icon, contentDescription = label)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    Card(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (note.pinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (note.title.isNotBlank()) Spacer(Modifier.height(4.dp))

            when (note.type) {
                NoteType.TEXT -> {
                    if (note.body.isNotBlank()) {
                        Text(
                            text = note.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                NoteType.CHECKLIST -> {
                    val preview = note.items.take(5)
                    preview.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 1.dp),
                        ) {
                            Icon(
                                if (item.checked) Icons.Default.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint =
                                    if (item.checked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                                color =
                                    if (item.checked) {
                                        MaterialTheme.colorScheme.outline
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                            )
                        }
                    }
                    if (note.items.size > 5) {
                        Text(
                            "+${note.items.size - 5} more",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                AnimatedVisibility(isSelected) {
                    Row {
                        IconButton(onClick = onPin, modifier = Modifier.size(28.dp)) {
                            Icon(
                                if (note.pinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.AutoMirrored.Outlined.StickyNote2,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "No notes yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                "Tap + to create one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

fun formatDate(ms: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - ms
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ms))
    }
}
