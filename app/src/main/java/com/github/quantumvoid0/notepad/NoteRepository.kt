package com.github.quantumvoid0.notepad

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notes")

class NoteRepository(private val context: Context) {

    private val gson = Gson()
    private val NOTES_KEY = stringPreferencesKey("notes_json")

    val notesFlow: Flow<List<Note>> = context.dataStore.data.map { prefs ->
        val json = prefs[NOTES_KEY] ?: return@map emptyList()
        val type = object : TypeToken<List<Note>>() {}.type
        runCatching { gson.fromJson<List<Note>>(json, type) }.getOrDefault(emptyList())
    }

    private suspend fun saveAll(notes: List<Note>) {
        context.dataStore.edit { prefs ->
            prefs[NOTES_KEY] = gson.toJson(notes)
        }
    }

    suspend fun upsert(note: Note, current: List<Note>): List<Note> {
        val updated = current.toMutableList()
        val idx = updated.indexOfFirst { it.id == note.id }
        if (idx >= 0) updated[idx] = note else updated.add(0, note)
        saveAll(updated)
        return updated
    }

    suspend fun delete(noteId: String, current: List<Note>): List<Note> {
        val updated = current.filterNot { it.id == noteId }
        saveAll(updated)
        return updated
    }

    suspend fun togglePin(noteId: String, current: List<Note>): List<Note> {
        val updated = current.map {
            if (it.id == noteId) it.copy(pinned = !it.pinned) else it
        }
        saveAll(updated)
        return updated
    }
}
