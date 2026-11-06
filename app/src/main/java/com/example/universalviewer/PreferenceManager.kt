package com.example.universalviewer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("UniversalViewerPrefs", Context.MODE_PRIVATE)

    fun addUsageTime(millis: Long) {
        val total = prefs.getLong("usage_time", 0) + millis
        prefs.edit().putLong("usage_time", total).apply()
    }

    fun getTotalUsageTime(): String {
        val totalMillis = prefs.getLong("usage_time", 0)
        val minutes = (totalMillis / 1000) / 60
        return "$minutes dəqiqə"
    }

    fun saveRecentFile(uri: Uri, fileName: String) {
        val list = getDocumentList().toMutableList()
        val uriString = uri.toString()
        
        val existingIndex = list.indexOfFirst { it.uri == uriString }
        if (existingIndex != -1) {
            val existing = list[existingIndex]
            existing.lastOpened = System.currentTimeMillis()
            list[existingIndex] = existing
        } else {
            list.add(0, DocumentItem(uriString, fileName))
        }

        val trimmedList = list.take(20)
        val json = Gson().toJson(trimmedList)
        prefs.edit().putString("document_list", json).apply()
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

    fun getSortedDocumentList(): List<DocumentItem> {
        val list = getDocumentList()
        return list.sortedWith(compareByDescending<DocumentItem> { it.isPinned }
            .thenByDescending { it.isFavorite }
            .thenByDescending { it.lastOpened })
    }
}
