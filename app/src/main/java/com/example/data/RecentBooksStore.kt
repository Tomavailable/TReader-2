package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class RecentBook(
    val id: String,
    val title: String,
    val snippet: String,
    val sentenceCount: Int,
    val lastIndex: Int = 0,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val isProcessing: Boolean = false,
    val progress: Float = 1f
)

class RecentBooksStore(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tts_recent_books", Context.MODE_PRIVATE)
    private val booksDir: File by lazy {
        File(context.filesDir, "books").apply { if (!exists()) mkdirs() }
    }

    fun getBookFile(id: String): File {
        return File(booksDir, "$id.txt")
    }

    fun getBookText(id: String): String {
        val bookFile = getBookFile(id)
        return if (bookFile.exists()) {
            try { bookFile.readText() } catch (e: Exception) { "" }
        } else {
            ""
        }
    }

    fun getRecentBooks(): List<RecentBook> {
        val jsonStr = prefs.getString(KEY_BOOKS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<RecentBook>()

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", System.currentTimeMillis().toString())
                val title = obj.optString("title", "未命名文档")
                val snippet = obj.optString("snippet", "")
                val sentenceCount = obj.optInt("sentenceCount", 0)
                val lastIndex = obj.optInt("lastIndex", 0)
                val lastReadTimestamp = obj.optLong("lastReadTimestamp", System.currentTimeMillis())
                val isProcessing = obj.optBoolean("isProcessing", false)
                val progress = obj.optDouble("progress", 1.0).toFloat()

                list.add(
                    RecentBook(
                        id = id,
                        title = title,
                        snippet = snippet,
                        sentenceCount = sentenceCount,
                        lastIndex = lastIndex,
                        lastReadTimestamp = lastReadTimestamp,
                        isProcessing = isProcessing,
                        progress = progress
                    )
                )
            }

            list.sortedByDescending { it.lastReadTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addPendingBook(title: String): RecentBook {
        val currentList = getRecentBooks().toMutableList()
        currentList.removeAll { it.title == title }
        val id = System.currentTimeMillis().toString()
        val book = RecentBook(
            id = id,
            title = title,
            snippet = "正在准备读取...",
            sentenceCount = 0,
            lastIndex = 0,
            lastReadTimestamp = System.currentTimeMillis(),
            isProcessing = true,
            progress = 0.15f
        )
        currentList.add(0, book)
        val trimmed = currentList.take(20)
        persistList(trimmed)
        return book
    }

    fun updateBookProgress(id: String, progress: Float, statusMessage: String? = null) {
        val currentList = getRecentBooks().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = currentList[index]
            currentList[index] = old.copy(
                progress = progress,
                snippet = statusMessage ?: old.snippet
            )
            persistList(currentList)
        }
    }

    fun completeBookImport(id: String, fullText: String, sentenceCount: Int, snippet: String) {
        try {
            getBookFile(id).writeText(fullText)
        } catch (e: Exception) {
            Log.e("RecentBooksStore", "Failed to write book file: ${e.message}")
        }
        val currentList = getRecentBooks().toMutableList()
        val index = currentList.indexOfFirst { it.id == id }
        if (index != -1) {
            val old = currentList[index]
            currentList[index] = old.copy(
                sentenceCount = sentenceCount,
                snippet = snippet,
                isProcessing = false,
                progress = 1.0f,
                lastReadTimestamp = System.currentTimeMillis()
            )
            persistList(currentList)
        }
    }

    fun saveRecentBook(title: String, fullText: String, sentenceCount: Int, lastIndex: Int = 0): String {
        if (fullText.isBlank()) return ""
        val currentList = getRecentBooks().toMutableList()
        val removed = currentList.filter { it.title == title }
        currentList.removeAll { it.title == title }
        removed.forEach { old ->
            try {
                File(booksDir, "${old.id}.cache").delete()
                File(booksDir, "${old.id}.cache.tmp").delete()
            } catch (_: Exception) {}
        }

        val id = System.currentTimeMillis().toString()
        try {
            getBookFile(id).writeText(fullText)
        } catch (e: Exception) {
            Log.e("RecentBooksStore", "Failed to write book file: ${e.message}")
        }

        val snippet = fullText.take(120).replace("\n", " ").trim()
        val newBook = RecentBook(
            id = id,
            title = title,
            snippet = snippet,
            sentenceCount = sentenceCount,
            lastIndex = lastIndex,
            lastReadTimestamp = System.currentTimeMillis(),
            isProcessing = false,
            progress = 1.0f
        )
        currentList.add(0, newBook)

        // Keep at most 20 recent books
        val trimmed = currentList.take(20)
        persistList(trimmed)
        return id
    }

    fun updateProgress(title: String, lastIndex: Int) {
        val currentList = getRecentBooks().toMutableList()
        val index = currentList.indexOfFirst { it.title == title }
        if (index != -1) {
            val old = currentList[index]
            currentList[index] = old.copy(lastIndex = lastIndex, lastReadTimestamp = System.currentTimeMillis())
            persistList(currentList)
        }
    }

    fun deleteRecentBook(id: String) {
        val currentList = getRecentBooks().toMutableList()
        currentList.removeAll { it.id == id }
        try {
            val file = getBookFile(id)
            if (file.exists()) file.delete()
            File(booksDir, "$id.cache").delete()
            File(booksDir, "$id.cache.tmp").delete()
        } catch (e: Exception) {
            // ignore
        }
        persistList(currentList)
    }

    private fun persistList(list: List<RecentBook>) {
        val jsonArray = JSONArray()
        list.forEach { book ->
            val obj = JSONObject().apply {
                put("id", book.id)
                put("title", book.title)
                put("snippet", book.snippet)
                put("sentenceCount", book.sentenceCount)
                put("lastIndex", book.lastIndex)
                put("lastReadTimestamp", book.lastReadTimestamp)
                put("isProcessing", book.isProcessing)
                put("progress", book.progress.toDouble())
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_BOOKS, jsonArray.toString()).apply()
    }

    companion object {
        private const val KEY_BOOKS = "recent_books_json"
    }
}
