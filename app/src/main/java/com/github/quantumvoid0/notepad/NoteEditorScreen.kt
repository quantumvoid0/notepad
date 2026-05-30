package com.github.quantumvoid0.notepad

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: Note?,
    defaultType: NoteType,
    onSave: (Note) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    //initialise state from existing note or blank
    val note = initialNote ?: Note(type = defaultType)
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }
    var type by remember { mutableStateOf(note.type) }
    var items by remember { mutableStateOf(note.items.ifEmpty { listOf(CheckItem()) }) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val bodyFocus = remember { FocusRequester() }

    //save on back
    fun persist() {
        val saved = note.copy(
            title = title.trim(),
            body = body,
            type = type,
            items = items.filter { it.text.isNotBlank() },
            updatedAt = System.currentTimeMillis()
        )
        //only save if theres actual content tho
        val hasContent = saved.title.isNotBlank() || saved.body.isNotBlank() || saved.items.isNotEmpty()
        if (hasContent) onSave(saved)
        onBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete note?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(note.id)
                        onBack()
                    }
                ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = ::persist) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {},
                actions = {
                    //toggle_type
                    IconButton(onClick = {
                        type = if (type == NoteType.TEXT) NoteType.CHECKLIST else NoteType.TEXT
                    }) {
                        Icon(
                            if (type == NoteType.TEXT) Icons.Outlined.CheckBox else Icons.Outlined.TextFields,
                            contentDescription = "Switch type"
                        )
                    }
                    //delete existing only
                    if (initialNote != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                //header
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text(
                                "Title",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        inner()
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
            }

            when (type) {
                NoteType.TEXT -> {
                    item {
                        BasicTextField(
                            value = body,
                            onValueChange = { body = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 300.dp)
                                .focusRequester(bodyFocus),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (body.isEmpty()) {
                                    Text(
                                        "Start writing…",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }

                NoteType.CHECKLIST -> {
                    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                        ChecklistRow(
                            item = item,
                            onCheckedChange = { checked ->
                                items = items.toMutableList().also { it[index] = item.copy(checked = checked) }
                            },
                            onTextChange = { text ->
                                items = items.toMutableList().also { it[index] = item.copy(text = text) }
                            },
                            onNext = {
                                //add new item after this one
                                val newItem = CheckItem()
                                items = items.toMutableList().also { it.add(index + 1, newItem) }
                            },
                            onDelete = {
                                if (items.size > 1) {
                                    items = items.toMutableList().also { it.removeAt(index) }
                                }
                            }
                        )
                    }
                    item {
                        TextButton(
                            onClick = { items = items + CheckItem() },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add item")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistRow(
    item: CheckItem,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(40.dp)
        )
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = if (item.checked) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.onSurface,
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                textDecoration = if (item.checked)
                    androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            decorationBox = { inner ->
                if (item.text.isEmpty()) {
                    Text("List item", color = MaterialTheme.colorScheme.outlineVariant,
                        style = MaterialTheme.typography.bodyLarge)
                }
                inner()
            },
            singleLine = true
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}
