package com.github.quantumvoid0.notepad

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WidgetConfigRepository(
    private val context: Context,
) {
    private val gson = Gson()
    private val KEY = stringPreferencesKey("widget_configs")

    private suspend fun loadAll(): MutableMap<Int, WidgetConfig> {
        val json =
            context.dataStore.data
                .map { it[KEY] }
                .first() ?: return mutableMapOf()
        val type = object : TypeToken<MutableMap<Int, WidgetConfig>>() {}.type
        return runCatching { gson.fromJson(json, type) as MutableMap<Int, WidgetConfig> }
            .getOrDefault(mutableMapOf())
    }

    private suspend fun saveAll(map: Map<Int, WidgetConfig>) {
        context.dataStore.edit { it[KEY] = gson.toJson(map) }
    }

    suspend fun get(appWidgetId: Int): WidgetConfig? = loadAll()[appWidgetId]

    suspend fun save(config: WidgetConfig) {
        val map = loadAll()
        map[config.appWidgetId] = config
        saveAll(map)
    }

    suspend fun delete(appWidgetId: Int) {
        val map = loadAll()
        map.remove(appWidgetId)
        saveAll(map)
    }
}
