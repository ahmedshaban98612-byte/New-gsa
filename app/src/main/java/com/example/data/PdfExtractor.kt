package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor

object PdfExtractor {
    private const val TAG = "PdfExtractor"

    fun extractTextFromPdf(context: Context, uri: Uri): String {
        return try {
            Log.d(TAG, "Initializing iText PDF reader...")
            context.contentResolver.openInputStream(uri).use { inputStream ->
                if (inputStream == null) {
                    throw Exception("فشل في قراءة مسار الملف")
                }
                
                val reader = PdfReader(inputStream)
                try {
                    val totalPages = reader.numberOfPages
                    Log.d(TAG, "PDF Loaded successfully with iText. Pages: $totalPages")
                    
                    val sb = StringBuilder()
                    // Limit text to first 10 pages to avoid hitting Gemini token limits or memory limits
                    val endPage = minOf(totalPages, 10)
                    for (i in 1..endPage) {
                        val pageText = PdfTextExtractor.getTextFromPage(reader, i)
                        sb.append(pageText).append("\n")
                    }
                    
                    val text = sb.toString()
                    Log.d(TAG, "Successfully extracted ${text.length} characters of text")
                    
                    if (text.trim().isEmpty()) {
                        throw Exception("الملف فارغ أو يحتوي على صور فقط وليس نصوص قابلة للاستخراج")
                    }
                    text
                } finally {
                    reader.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF", e)
            throw Exception("فشل استخراج النصوص من ملف الـ PDF: ${e.message}")
        }
    }
}
