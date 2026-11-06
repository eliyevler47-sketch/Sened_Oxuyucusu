package com.example.universalviewer

data class DocumentItem(
    val uri: String,
    val fileName: String,
    var isPinned: Boolean = false,
    var isFavorite: Boolean = false,
    var lastOpened: Long = System.currentTimeMillis()
)
