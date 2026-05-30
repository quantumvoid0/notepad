package com.github.quantumvoid0.notepad

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.*
import android.view.View
import android.widget.RemoteViews

object WidgetUpdater {
    suspend fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
    ) {
        val configRepo = WidgetConfigRepository(context)
        val noteRepo = NoteRepository(context)

        val config = configRepo.get(appWidgetId) ?: return
        val notes = noteRepo.getAll()
        val views = RemoteViews(context.packageName, R.layout.widget_note)
	
        val note =
            notes.find { it.id == config.noteId } ?: run {
                views.setViewVisibility(R.id.widget_title, View.VISIBLE)
                views.setTextViewText(R.id.widget_title, "Note/List was deleted")
                views.setViewVisibility(R.id.widget_divider, View.GONE)
                views.setViewVisibility(R.id.widget_body, View.GONE)
                views.setViewVisibility(R.id.widget_checklist, View.GONE)
                views.setRemoteAdapter(
                    R.id.widget_checklist,
                    android.content.Intent(context, WidgetRemoteViewsService::class.java),
                )
                applyDynamicBg(views, config, context)
                val intent =
                    android.content.Intent(context, MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                val pi =
                    android.app.PendingIntent.getActivity(
                        context,
                        appWidgetId + 2000,
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_IMMUTABLE,
                    )
                views.setOnClickPendingIntent(R.id.widget_title, pi)
                appWidgetManager.updateAppWidget(appWidgetId, views)
                return
            }	

        // dynamic background bitmap
        views.setInt(R.id.widget_container, "setBackgroundColor", 0) // clear static bg

        // apply dynamic background as a GradientDrawable via bitmap trick
        applyDynamicBg(views, config, context)
        views.setOnClickPendingIntent(R.id.widget_container, null)

        // head
        if (note.title.isNotBlank()) {
            views.setViewVisibility(R.id.widget_title, View.VISIBLE)
            views.setViewVisibility(R.id.widget_divider, View.VISIBLE)
            views.setTextViewText(R.id.widget_title, note.title)
        } else {
            views.setViewVisibility(R.id.widget_title, View.GONE)
            views.setViewVisibility(R.id.widget_divider, View.GONE)
        }

        val p = config.paddingDp.dpToPx(context)
        views.setViewPadding(R.id.widget_container, p, p, p, p)

        // tap title to open note
	
        val titleIntent =
            android.content.Intent(context, MainActivity::class.java).apply {
                putExtra("open_note_id", note.id)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
	
        val titlePi =
            android.app.PendingIntent.getActivity(
                context,
                appWidgetId + 1000,
                titleIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE,
            )
        views.setOnClickPendingIntent(R.id.widget_title, titlePi)

        when (note.type) {
            NoteType.TEXT -> {
                views.setViewVisibility(R.id.widget_body, View.VISIBLE)
                views.setViewVisibility(R.id.widget_checklist, View.GONE)
                views.setRemoteAdapter(R.id.widget_checklist, android.content.Intent(context, WidgetRemoteViewsService::class.java))
                views.setTextViewText(R.id.widget_body, note.body.ifBlank { "Empty note" })
            }	

            NoteType.CHECKLIST -> {
                views.setViewVisibility(R.id.widget_body, View.GONE)
                views.setViewVisibility(R.id.widget_checklist, View.VISIBLE)

                val serviceIntent =
                    android.content.Intent(context, WidgetRemoteViewsService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra("note_id", note.id)
                        data = android.net.Uri.parse(toUri(android.content.Intent.URI_INTENT_SCHEME))
                    }
                views.setRemoteAdapter(R.id.widget_checklist, serviceIntent)

                // item click = broadcast update
                val itemIntent =
                    android.content.Intent(context, NoteWidgetProvider::class.java).apply {
                        action = NoteWidgetProvider.ACTION_TOGGLE_ITEM
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra("note_id", note.id)
                    }
                val itemPi =
                    android.app.PendingIntent.getBroadcast(
                        context,
                        appWidgetId,
                        itemIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_MUTABLE,
                    )
                views.setPendingIntentTemplate(R.id.widget_checklist, itemPi)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // eye candy
    private fun applyDynamicBg(
        views: RemoteViews,
        config: WidgetConfig,
        context: Context,
    ) {
        val width = 800
        val height = 400
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
	
        val bgColor =
            (config.bgColor.toInt() and 0x00FFFFFF) or
                ((config.bgAlpha shl 24) and 0xFF000000.toInt())
        val borderColor = config.borderColor.toInt() or 0xFF000000.toInt()
        val radius = config.borderRadius.dpToPx(context).toFloat()
        val borderWidth = config.borderWidth.dpToPx(context).toFloat()
	
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.style = Paint.Style.FILL
        paint.color = bgColor
        val fillRect =
            RectF(
                borderWidth / 2,
                borderWidth / 2,
                width - borderWidth / 2,
                height - borderWidth / 2,
            )
        canvas.drawRoundRect(fillRect, radius, radius, paint)
	
        if (borderWidth > 0) {
            paint.style = Paint.Style.STROKE
            paint.color = borderColor
            paint.strokeWidth = borderWidth
            canvas.drawRoundRect(fillRect, radius, radius, paint)
        }
	
        views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
        views.setInt(R.id.widget_container, "setBackgroundColor", 0x00000000)
    }
}

private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()
