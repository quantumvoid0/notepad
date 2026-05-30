package com.github.quantumvoid0.notepad

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // set result CANCELED in case user backs out
        setResult(RESULT_CANCELED)

        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            NotepadTheme {
                WidgetConfigScreen(
                    appWidgetId = appWidgetId,
                    onConfirm = { config ->
                        // save config FIRST on IO, then update, then set result and finish on Main
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                            WidgetConfigRepository(this@WidgetConfigActivity).save(config)
                            val mgr = AppWidgetManager.getInstance(this@WidgetConfigActivity)
                            WidgetUpdater.update(this@WidgetConfigActivity, mgr, appWidgetId)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                val resultIntent =
                                    Intent().apply {
                                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                    }
                                setResult(RESULT_OK, resultIntent)
                                finish()
                            }
                        }
                    },
                    onCancel = { finish() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    appWidgetId: Int,
    onConfirm: (WidgetConfig) -> Unit,
    onCancel: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val noteRepo = remember { NoteRepository(context) }
    val notes by noteRepo.notesFlow.collectAsState(initial = emptyList())

    var selectedNoteId by remember { mutableStateOf<String?>(null) }

    // appearance state
    var bgAlpha by remember { mutableFloatStateOf(0.9f) }
    var bgColorHex by remember { mutableStateOf("1C1C1C") }
    var borderColorHex by remember { mutableStateOf("FFFFFF") }
    var borderWidth by remember { mutableFloatStateOf(2f) }
    var borderRadius by remember { mutableFloatStateOf(16f) }
    var paddingDp by remember { mutableFloatStateOf(12f) }

    val canConfirm = selectedNoteId != null

    // preview colors
    val bgColor = parseHexColor(bgColorHex, 0x1C1C1C)
    val borderColor = parseHexColor(borderColorHex, 0xFFFFFF)
    val previewBg = Color(bgColor).copy(alpha = bgAlpha)
    val previewBorder = Color(borderColor)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure Widget", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                },
                actions = {
                    Button(
                        onClick = {
                            val config =
                                WidgetConfig(
                                    appWidgetId = appWidgetId,
                                    noteId = selectedNoteId!!,
                                    bgColor = bgColor.toLong() and 0xFFFFFFFFL,
                                    bgAlpha = (bgAlpha * 255).toInt(),
                                    borderColor = borderColor.toLong() and 0xFFFFFFFFL,
                                    borderWidth = borderWidth.toInt(),
                                    borderRadius = borderRadius.toInt(),
                                    paddingDp = paddingDp.toInt(),
                                )
                            onConfirm(config)
                        },
                        enabled = canConfirm,
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text("Add") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            // widget preview
            SectionHeader("Preview")
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(140.dp)
                        .clip(RoundedCornerShape(borderRadius.dp))
                        .background(previewBg)
                        .border(borderWidth.dp, previewBorder, RoundedCornerShape(borderRadius.dp))
                        .padding(paddingDp.dp),
                contentAlignment = Alignment.TopStart,
            ) {
                val note = notes.find { it.id == selectedNoteId }
                if (note != null) {
                    Column {
                        if (note.title.isNotBlank()) {
                            Text(
                                note.title,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = Color.White.copy(alpha = 0.2f),
                            )
                        }
                        when (note.type) {
                            NoteType.TEXT -> {
                                Text(
                                    note.body.ifBlank { "Empty note" },
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            NoteType.CHECKLIST -> {
                                note.items.take(4).forEach { item ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.CheckBox,
                                            null,
                                            modifier = Modifier.size(14.dp),
                                            tint =
                                                if (item.checked) {
                                                    Color(0xFFFFFFFF)
                                                } else {
                                                    Color.White.copy(alpha = 0.5f)
                                                },
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            item.text,
                                            color = Color.White.copy(alpha = 0.8f),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        "Select a note below",
                        color = Color.White.copy(alpha = 0.4f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // note picker
            SectionHeader("Note")
            if (notes.isEmpty()) {
                Text(
                    "No notes yet, create one first",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                notes.forEach { note ->
                    NotePickerRow(
                        note = note,
                        selected = selectedNoteId == note.id,
                        onClick = { selectedNoteId = note.id },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // appearance
            SectionHeader("Background")
            SliderRow("Opacity", bgAlpha, 0f..1f) { bgAlpha = it }
            HexColorRow("Color", bgColorHex) { bgColorHex = it }

            SectionHeader("Border")
            HexColorRow("Color", borderColorHex) { borderColorHex = it }
            SliderRow("Width", borderWidth, 0f..8f, steps = 7) { borderWidth = it }
            SliderRow("Radius", borderRadius, 0f..32f, steps = 31) { borderRadius = it }

            SectionHeader("Padding")
            SliderRow("Size", paddingDp, 0f..32f, steps = 31) { paddingDp = it }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun NotePickerRow(
    note: Note,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (note.type == NoteType.CHECKLIST) Icons.Outlined.CheckBox else Icons.Outlined.TextFields,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                note.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val preview =
                when (note.type) {
                    NoteType.TEXT -> note.body.take(60)
                    NoteType.CHECKLIST -> note.items.take(2).joinToString(" · ") { it.text }
                }
            if (preview.isNotBlank()) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp),
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f),
        )
        Text(
            if (range.endInclusive <= 1f) {
                "%.0f%%".format(value * 100)
            } else {
                "%.0f".format(value)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(36.dp),
        )
    }
}

@Composable
private fun HexColorRow(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    val color =
        runCatching {
            Color(android.graphics.Color.parseColor("#$value"))
        }.getOrElse { MaterialTheme.colorScheme.surface }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(72.dp),
        )
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 6) onChange(it.uppercase()) },
            prefix = { Text("#") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                ),
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        )
    }
}

private fun parseHexColor(
    hex: String,
    fallback: Int,
): Int =
    runCatching {
        android.graphics.Color.parseColor("#$hex")
    }.getOrDefault(fallback)
