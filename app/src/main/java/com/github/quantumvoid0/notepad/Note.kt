package com.github.quantumvoid0.notepad

import java.util.UUID

enum class NoteType { TEXT, CHECKLIST }

data class CheckItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val checked: Boolean = false,
)

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val type: NoteType = NoteType.TEXT,
    val items: List<CheckItem> = emptyList(),
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
