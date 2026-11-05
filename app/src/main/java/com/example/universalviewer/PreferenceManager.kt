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

    fun saveRecentFile(uri: Uri) {
        val recentList = getRecentFiles().toMutableList()
        val uriString = uri.toString()
        recentList.remove(uriString)
        recentList.add(0, uriString)
        val trimmedList = recentList.take(5)
        val json = Gson().toJson(trimmedList)
        prefs.edit().putString("recent_files", json).apply()
    }

    fun getRecentFiles(): List<String> {
        val json = prefs.getString("recent_files", "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }
}
