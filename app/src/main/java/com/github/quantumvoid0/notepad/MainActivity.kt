package com.github.quantumvoid0.notepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val THEME_KEY = stringPreferencesKey("theme_mode")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appInfo = AppInfo(
            appName     = getString(R.string.app_name),
            packageName = packageName,
            versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0",
            versionCode = packageManager.getPackageInfo(packageName, 0).longVersionCode.toInt(),
            compileSdk  = applicationInfo.targetSdkVersion,
            minSdk      = applicationInfo.minSdkVersion,
            targetSdk   = applicationInfo.targetSdkVersion,
            buildType   = if (BuildConfig.DEBUG) "debug" else "release",
            githubUrl   = "https://github.com/quantumvoid0/notepad"
        )

        setContent {
            //read persisted theme, default to system
            val systemDefault = if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
            val themeModeStr by dataStore.data
                .map { it[THEME_KEY] }
                .collectAsState(initial = null)

            val themeMode = when (themeModeStr) {
                "LIGHT"  -> ThemeMode.LIGHT
                "DARK"   -> ThemeMode.DARK
                "AMOLED" -> ThemeMode.AMOLED
                else     -> systemDefault
            }

            val scope = rememberCoroutineScope()

            NotepadTheme(themeMode = themeMode) {
                NotepadApp(
                    appInfo     = appInfo,
                    themeMode   = themeMode,
                    onThemeChange = { mode ->
                        scope.launch {
                            dataStore.edit { it[THEME_KEY] = mode.name }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NotepadApp(
    vm: NoteViewModel = viewModel(),
    appInfo: AppInfo,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val state by vm.state.collectAsState()
    val notes by vm.filteredNotes.collectAsState()

    var pendingType by remember { mutableStateOf(NoteType.TEXT) }
    var showAbout   by remember { mutableStateOf(false) }

    when (val screen = state.screen) {
        is Screen.List -> {
            NoteListScreen(
                notes         = notes,
                searchQuery   = state.searchQuery,
                onSearchChange = vm::setSearch,
                onNoteClick   = { vm.openEditor(it) },
                onNewNote     = { type ->
                    pendingType = type
                    vm.openEditor(null)
                },
                onDelete = vm::deleteNote,
                onPin    = vm::togglePin,
                onAbout  = { showAbout = true }
            )
        }
        is Screen.Editor -> {
            val existingNote = vm.getNoteById(screen.noteId)
            BackHandler { }
            NoteEditorScreen(
                initialNote = existingNote,
                defaultType = pendingType,
                onSave      = vm::saveNote,
                onDelete    = vm::deleteNote,
                onBack      = vm::closeEditor
            )
        }
    }

    if (showAbout) {
        AboutSheet(
            info          = appInfo,
            themeMode     = themeMode,
            onThemeChange = onThemeChange,
            onDismiss     = { showAbout = false }
        )
    }
}
