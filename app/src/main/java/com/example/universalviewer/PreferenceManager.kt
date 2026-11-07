package com.example.universalviewer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("UniversalViewerPrefs", Context.MODE_PRIVATE)

    // ==================== USAGE TIME ====================
    fun addUsageTime(millis: Long) {
        if (millis <= 0) return
        val total = prefs.getLong("usage_time", 0) + millis
        prefs.edit().putLong("usage_time", total).apply()
    }

    fun getTotalUsageTime(): String {
        val totalMillis = prefs.getLong("usage_time", 0)
        val minutes = (totalMillis / 1000) / 60
        return "$minutes dəqiqə"
    }

    fun getTotalUsageMs(): Long = prefs.getLong("usage_time", 0)

    fun resetUsageTime() {
        prefs.edit().putLong("usage_time", 0L).apply()
    }

    // ==================== RECENT FILES ====================
    fun saveRecentFile(uri: Uri, fileName: String) {
        val list = getDocumentList().toMutableList()
        val uriString = uri.toString()

        val existingIndex = list.indexOfFirst { it.uri == uriString }
        if (existingIndex != -1) {
            val existing = list[existingIndex]
            existing.lastOpened = System.currentTimeMillis()
            list.removeAt(existingIndex)
            list.add(0, existing)
        } else {
            list.add(0, DocumentItem(uriString, fileName))
        }

        val trimmedList = list.take(20)
        prefs.edit().putString("document_list", Gson().toJson(trimmedList)).apply()
    }

    fun getDocumentList(): List<DocumentItem> {
        val json = prefs.getString("document_list", "[]")
        val type = object : TypeToken<List<DocumentItem>>() {}.type
        return try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getSortedDocumentList(): List<DocumentItem> {
        return getDocumentList().sortedWith(
            compareByDescending<DocumentItem> { it.isPinned }
                .thenByDescending { it.isFavorite }
                .thenByDescending { it.lastOpened }
        )
    }

    fun togglePin(uri: String) {
        val list = getDocumentList().toMutableList()
        val index = list.indexOfFirst { it.uri == uri }
        if (index != -1) {
            list[index].isPinned = !list[index].isPinned
            prefs.edit().putString("document_list", Gson().toJson(list)).apply()
        }
    }

    fun toggleFavorite(uri: String) {
        val list = getDocumentList().toMutableList()
        val index = list.indexOfFirst { it.uri == uri }
        if (index != -1) {
            list[index].isFavorite = !list[index].isFavorite
            prefs.edit().putString("document_list", Gson().toJson(list)).apply()
        }
    }

    fun removeRecent(uri: String) {
        val list = getDocumentList().toMutableList()
        list.removeAll { it.uri == uri }
        prefs.edit().putString("document_list", Gson().toJson(list)).apply()
    }

    fun clearRecent() {
        prefs.edit().remove("document_list").apply()
    }

    // ==================== FONT SIZE ====================
    // 0 = Kiçik, 1 = Orta, 2 = Böyük
    fun getFontSize(): Int = prefs.getInt("font_size", 1)

    fun setFontSize(value: Int) {
        prefs.edit().putInt("font_size", value.coerceIn(0, 2)).apply()
    }
}
