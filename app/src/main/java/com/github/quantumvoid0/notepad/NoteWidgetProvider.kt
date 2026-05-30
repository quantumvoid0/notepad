package com.github.quantumvoid0.notepad

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            CoroutineScope(Dispatchers.IO).launch {
                WidgetUpdater.update(context, appWidgetManager, id)
            }
        }
    }

    override fun onDeleted(
        context: Context,
        appWidgetIds: IntArray,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val repo = WidgetConfigRepository(context)
            appWidgetIds.forEach { repo.delete(it) }
        }
    }

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_ITEM) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val noteId = intent.getStringExtra("note_id") ?: return
            val itemId = intent.getStringExtra("item_id") ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val noteRepo = NoteRepository(context)
                noteRepo.toggleCheckItem(noteId, itemId)

                // /notify the list adapter to refresh
                val manager = AppWidgetManager.getInstance(context)
                manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_checklist)
            }
        }

        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            if (appWidgetId == -1) return
            val manager = AppWidgetManager.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                WidgetUpdater.update(context, manager, appWidgetId)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ITEM = "com.github.quantumvoid0.notepad.TOGGLE_ITEM"
        const val ACTION_REFRESH = "com.github.quantumvoid0.notepad.REFRESH_WIDGET"
    }
}
