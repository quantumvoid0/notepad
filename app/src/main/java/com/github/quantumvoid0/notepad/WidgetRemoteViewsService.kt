package com.github.quantumvoid0.notepad

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import kotlinx.coroutines.runBlocking

class WidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = ChecklistRemoteViewsFactory(applicationContext, intent)
}

class ChecklistRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent,
) : RemoteViewsService.RemoteViewsFactory {
    private val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
    private val noteId = intent.getStringExtra("note_id") ?: ""
    private var items: List<CheckItem> = emptyList()

    override fun onCreate() {
        load()
    }

    override fun onDataSetChanged() {
        load()
    }

    override fun onDestroy() {}

    private fun load() {
        items =
            runBlocking {
                NoteRepository(context)
                    .getAll()
                    .find { it.id == noteId }
                    ?.items ?: emptyList()
            }
    }

    override fun getCount() = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_checklist_item)
        views.setTextViewText(R.id.item_text, item.text)
        views.setCompoundButtonChecked(R.id.item_checkbox, item.checked)

        // strike thro for checked items
        val flag =
            if (item.checked) {
                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                0
            }
        views.setInt(R.id.item_text, "setPaintFlags", flag or android.graphics.Paint.ANTI_ALIAS_FLAG)

        val textColor = if (item.checked) 0x88AAAAAA.toInt() else 0xCCE0E0E0.toInt()
        views.setTextColor(R.id.item_text, textColor)

        // fill intent so the pending intent template gets the item id
        val fillIn =
            Intent().apply {
                putExtra("item_id", item.id)
                putExtra("note_id", noteId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
        views.setOnClickFillInIntent(R.id.item_checkbox, fillIn)
        views.setOnClickFillInIntent(R.id.item_text, fillIn)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount() = 1

    override fun getItemId(position: Int) = items[position].id.hashCode().toLong()

    override fun hasStableIds() = true
}
