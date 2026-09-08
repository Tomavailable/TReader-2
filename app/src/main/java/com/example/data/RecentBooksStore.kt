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
    val fullText: String = "",
    val sentenceCount: Int,
    val lastIndex: Int = 0,
    val lastReadTimestamp: Long = System.currentTimeMillis()
)

class RecentBooksStore(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tts_recent_books", Context.MODE_PRIVATE)
    private val booksDir: File by lazy {
        File(context.filesDir, "books").apply { if (!exists()) mkdirs() }
    }

    private fun getBookFile(id: String): File {
        return File(booksDir, "$id.txt")
    }

    fun getRecentBooks(): List<RecentBook> {
        val jsonStr = prefs.getString(KEY_BOOKS, null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<RecentBook>()
            var needsMigration = false

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", System.currentTimeMillis().toString())
                val title = obj.optString("title", "未命名文档")
                val snippet = obj.optString("snippet", "")
                var legacyFullText = obj.optString("fullText", "")
                val sentenceCount = obj.optInt("sentenceCount", 0)
                val lastIndex = obj.optInt("lastIndex", 0)
                val lastReadTimestamp = obj.optLong("lastReadTimestamp", System.currentTimeMillis())

                val bookFile = getBookFile(id)
                val textContent = when {
                    bookFile.exists() -> {
                        try { bookFile.readText() } catch (e: Exception) { legacyFullText }
                    }
                    legacyFullText.isNotBlank() -> {
                        // Migrate legacy fullText from SharedPreferences to disk file
                        try {
                            bookFile.writeText(legacyFullText)
                            needsMigration = true
                        } catch (e: Exception) {
                            Log.w("RecentBooksStore", "Failed to migrate book file: ${e.message}")
                        }
                        legacyFullText
                    }
                    else -> ""
                }

                list.add(
                    RecentBook(
                        id = id,
                        title = title,
                        snippet = snippet,
                        fullText = textContent,
                        sentenceCount = sentenceCount,
                        lastIndex = lastIndex,
                        lastReadTimestamp = lastReadTimestamp
                    )
                )
            }

            if (needsMigration) {
                persistList(list)
            }

            list.sortedByDescending { it.lastReadTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRecentBook(title: String, fullText: String, sentenceCount: Int, lastIndex: Int = 0) {
        if (fullText.isBlank()) return
        val currentList = getRecentBooks().toMutableList()
        // Remove existing if matching same title or text
        currentList.removeAll { it.title == title || it.fullText == fullText }

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
            fullText = fullText,
            sentenceCount = sentenceCount,
            lastIndex = lastIndex,
            lastReadTimestamp = System.currentTimeMillis()
        )
        currentList.add(0, newBook)

        // Keep at most 15 recent books
        val trimmed = currentList.take(15)
        persistList(trimmed)
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
                // Do NOT write multi-megabyte fullText into SharedPreferences XML!
                put("sentenceCount", book.sentenceCount)
                put("lastIndex", book.lastIndex)
                put("lastReadTimestamp", book.lastReadTimestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_BOOKS, jsonArray.toString()).apply()
    }

    companion object {
        private const val KEY_BOOKS = "recent_books_json"
    }
}
