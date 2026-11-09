package com.example.universalviewer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("UniversalViewerPrefs", Context.MODE_PRIVATE)

    fun addDocumentUsageTime(uriString: String, millis: Long) {
        if (millis <= 0) return
        val list = getDocumentList().toMutableList()
        val index = list.indexOfFirst { it.uri == uriString }
        if (index != -1) {
            list[index].timeSpentMs += millis
            prefs.edit().putString("document_list", Gson().toJson(list)).apply()
        }
    }

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
        return try { Gson().fromJson(json, type) ?: emptyList() } catch (e: Exception) { emptyList() }
    }

    fun getSortedDocumentList(): List<DocumentItem> {
        return getDocumentList().sortedWith(
            compareByDescending<DocumentItem> { it.isPinned }
                .thenByDescending { it.lastOpened }
        )
    }

    // ==================== BOOKMARKS (ƏLƏFÇİNLƏR) ====================
    fun toggleBookmark(uri: String) {
        val list = getDocumentList().toMutableList()
        val index = list.indexOfFirst { it.uri == uri }
        if (index != -1) {
            list[index].isFavorite = !list[index].isFavorite // isFavorite-i bookmark kimi istifadə edirik
            prefs.edit().putString("document_list", Gson().toJson(list)).apply()
        }
    }

    fun getBookmarkedDocuments(): List<DocumentItem> {
        return getDocumentList().filter { it.isFavorite }
    }

    fun removeRecent(uri: String) {
        val list = getDocumentList().toMutableList()
        list.removeAll { it.uri == uri }
        prefs.edit().putString("document_list", Gson().toJson(list)).apply()
    }

    fun clearRecent() {
        prefs.edit().remove("document_list").apply()
    }
}
