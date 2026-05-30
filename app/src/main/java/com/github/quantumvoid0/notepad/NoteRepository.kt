package com.github.quantumvoid0.notepad

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

val Context.dataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> by androidx.datastore.preferences
    .preferencesDataStore(
        name = "notes",
    )

class NoteRepository(
    private val context: Context,
) {
    private val gson = Gson()
    private val NOTES_KEY = stringPreferencesKey("notes_json")

    val notesFlow: Flow<List<Note>> =
        context.dataStore.data.map { prefs ->
            val json = prefs[NOTES_KEY] ?: return@map emptyList()
            val type = object : TypeToken<List<Note>>() {}.type
            runCatching { gson.fromJson(json, type) as List<Note> }.getOrDefault(emptyList())
        }

    suspend fun getAll(): List<Note> = notesFlow.first()

    private suspend fun saveAll(notes: List<Note>) {
        context.dataStore.edit { prefs ->
            prefs[NOTES_KEY] = gson.toJson(notes)
        }
        notifyWidgets()
    }

    private fun notifyWidgets() {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val ids =
            manager.getAppWidgetIds(
                android.content.ComponentName(context, NoteWidgetProvider::class.java),
            )
        if (ids.isEmpty()) return
        val intent =
            android.content.Intent(context, NoteWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
        context.sendBroadcast(intent)
        manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_checklist)
    }

    suspend fun upsert(
        note: Note,
        current: List<Note>,
    ): List<Note> {
        val updated = current.toMutableList()
        val idx = updated.indexOfFirst { it.id == note.id }
        if (idx >= 0) updated[idx] = note else updated.add(0, note)
        saveAll(updated)
        return updated
    }

    suspend fun delete(
        noteId: String,
        current: List<Note>,
    ): List<Note> {
        val updated = current.filterNot { it.id == noteId }
        saveAll(updated)
        removeWidgetsForNote(noteId)
        return updated
    }

    private suspend fun removeWidgetsForNote(noteId: String) {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val ids =
            manager.getAppWidgetIds(
                android.content.ComponentName(context, NoteWidgetProvider::class.java),
            )
        val configRepo = WidgetConfigRepository(context)
        ids.forEach { id ->
            val config = configRepo.get(id)
            if (config?.noteId == noteId) {
                configRepo.delete(id)
            }
        }
    }

    suspend fun togglePin(
        noteId: String,
        current: List<Note>,
    ): List<Note> {
        val updated =
            current.map {
                if (it.id == noteId) it.copy(pinned = !it.pinned) else it
            }
        saveAll(updated)
        return updated
    }

    suspend fun toggleCheckItem(
        noteId: String,
        itemId: String,
    ): List<Note> {
        val notes = getAll().toMutableList()
        val idx = notes.indexOfFirst { it.id == noteId }
        if (idx < 0) return notes
        val note = notes[idx]
        val updatedItems =
            note.items.map {
                if (it.id == itemId) it.copy(checked = !it.checked) else it
            }
        notes[idx] = note.copy(items = updatedItems, updatedAt = System.currentTimeMillis())
        saveAll(notes)
        return notes
    }
}
