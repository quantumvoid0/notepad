package com.github.quantumvoid0.notepad

data class WidgetConfig(
    val appWidgetId: Int,
    val noteId: String,
    // bakground
    val bgColor: Long = 0xFF1C1C1C,
    val bgAlpha: Int = 230,
    // border
    val borderColor: Long = 0xFFFFFFF,
    val borderWidth: Int = 2,
    val borderRadius: Int = 16,
    // padding
    val paddingDp: Int = 12,
)
