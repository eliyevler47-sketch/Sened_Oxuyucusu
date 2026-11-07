package com.example.universalviewer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.documentfile.provider.DocumentFile
import com.example.universalviewer.*
import java.io.File

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

    fun copyUriToCache(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}")
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun routeFile(context: Context, uri: Uri) {
        val fileName = getFileName(context, uri)
        
        val prefManager = PreferenceManager(context)
        prefManager.saveRecentFile(uri, fileName)

        val tempFile = copyUriToCache(context, uri)
        if (tempFile == null) {
            Toast.makeText(context, "Fayl oxuna bilmədi.", Toast.LENGTH_SHORT).show()
            return
        }

        val mimeType = context.contentResolver.getType(uri)
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK

        try {
            when {
                mimeType?.contains("pdf") == true || fileName.lowercase().endsWith(".pdf") -> {
                    context.startActivity(Intent(context, PdfViewerActivity::class.java).apply { 
                        data = Uri.fromFile(tempFile)
                        addFlags(flags)
                        putExtra("FILE_NAME", fileName) 
                    })
                }
                fileName.lowercase().endsWith(".docx") || fileName.lowercase().endsWith(".xlsx") || fileName.lowercase().endsWith(".pptx") -> {
                    context.startActivity(Intent(context, OfficeViewerActivity::class.java).apply { 
                        data = Uri.fromFile(tempFile)
                        addFlags(flags)
                        putExtra("FILE_NAME", fileName) 
                    })
                }
                mimeType?.contains("html") == true || mimeType?.contains("text") == true || fileName.lowercase().endsWith(".txt") -> {
                    context.startActivity(Intent(context, HtmlViewerActivity::class.java).apply { 
                        data = Uri.fromFile(tempFile)
                        addFlags(flags)
                        putExtra("FILE_NAME", fileName) 
                    })
                }
                mimeType?.startsWith("image/") == true -> {
                    context.startActivity(Intent(context, ImageViewerActivity::class.java).apply { 
                        data = Uri.fromFile(tempFile)
                        addFlags(flags)
                        putExtra("FILE_NAME", fileName) 
                    })
                }
                else -> {
                    Toast.makeText(context, "Bu fayl növü dəstəklənmir.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Xəta: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
