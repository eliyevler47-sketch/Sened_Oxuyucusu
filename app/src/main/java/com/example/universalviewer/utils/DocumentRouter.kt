package com.example.universalviewer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.example.universalviewer.*
import com.example.universalviewer.data.PreferenceManager

object DocumentRouter {
    
    fun getFileName(context: Context, uri: Uri): String {
        var fileName = DocumentFile.fromSingleUri(context, uri)?.name
        if (fileName.isNullOrBlank()) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) fileName = cursor.getString(index)
                cursor.close()
            }
        }
        return fileName ?: "Sənəd"
    }

    fun routeFile(context: Context, uri: Uri) {
        val fileName = getFileName(context, uri)
        val prefManager = PreferenceManager(context)
        prefManager.saveRecentFile(uri, fileName)

        val mimeType = context.contentResolver.getType(uri)
        
        when {
            mimeType?.contains("pdf") == true || fileName.lowercase().endsWith(".pdf") -> {
                context.startActivity(Intent(context, PdfViewerActivity::class.java).apply { 
                    data = uri
                    putExtra("FILE_NAME", fileName) 
                })
            }
            fileName.lowercase().endsWith(".docx") || fileName.lowercase().endsWith(".xlsx") || fileName.lowercase().endsWith(".pptx") -> {
                context.startActivity(Intent(context, OfficeViewerActivity::class.java).apply { 
                    data = uri
                    putExtra("FILE_NAME", fileName) 
                })
            }
            mimeType?.contains("html") == true || mimeType?.contains("text") == true || fileName.lowercase().endsWith(".txt") -> {
                context.startActivity(Intent(context, HtmlViewerActivity::class.java).apply { 
                    data = uri
                    putExtra("FILE_NAME", fileName) 
                })
            }
            mimeType?.startsWith("image/") == true -> {
                context.startActivity(Intent(context, ImageViewerActivity::class.java).apply { 
                    data = uri
                    putExtra("FILE_NAME", fileName) 
                })
            }
            else -> {
                Toast.makeText(context, "Bu fayl növü dəstəklənmir.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
