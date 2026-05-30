package com.github.quantumvoid0.notepad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val notes: List<Note> = emptyList(),
    val searchQuery: String = "",
    val screen: Screen = Screen.List
)

sealed class Screen {
    object List : Screen()
    data class Editor(val noteId: String?) : Screen()   //null = new note
}

class NoteViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = NoteRepository(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.notesFlow.collect { notes ->
                _state.update { it.copy(notes = notes) }
            }
        }
    }

    val filteredNotes: StateFlow<List<Note>> = _state
        .map { s ->
            val q = s.searchQuery.trim().lowercase()
            val list = if (q.isEmpty()) s.notes
            else s.notes.filter { n ->
                n.title.lowercase().contains(q) || n.body.lowercase().contains(q) ||
                    n.items.any { it.text.lowercase().contains(q) }
            }
            val pinned = list.filter { it.pinned }
            val rest   = list.filterNot { it.pinned }
            pinned + rest
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setSearch(q: String) = _state.update { it.copy(searchQuery = q) }

    fun openEditor(noteId: String?) = _state.update { it.copy(screen = Screen.Editor(noteId)) }

    fun closeEditor() = _state.update { it.copy(screen = Screen.List) }

    fun saveNote(note: Note) = viewModelScope.launch {
        val updated = note.copy(updatedAt = System.currentTimeMillis())
        val newList = repo.upsert(updated, _state.value.notes)
        _state.update { it.copy(notes = newList) }
    }

    fun deleteNote(noteId: String) = viewModelScope.launch {
        val newList = repo.delete(noteId, _state.value.notes)
        _state.update { it.copy(notes = newList) }
    }

    fun togglePin(noteId: String) = viewModelScope.launch {
        val newList = repo.togglePin(noteId, _state.value.notes)
        _state.update { it.copy(notes = newList) }
    }

    fun getNoteById(id: String?): Note? = _state.value.notes.find { it.id == id }
}
