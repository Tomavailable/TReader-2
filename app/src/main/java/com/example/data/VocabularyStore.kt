package com.example.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class VocabularyItem(
    val id: Long = System.currentTimeMillis(),
    val word: String,
    val contextSentence: String = "",
    val bookTitle: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class VocabularyStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tts_vocabulary_store", Context.MODE_PRIVATE)

    @Synchronized
    fun getWords(): List<VocabularyItem> {
        val json = prefs.getString("saved_vocabularies", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<VocabularyItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    VocabularyItem(
                        id = obj.optLong("id", System.currentTimeMillis()),
                        word = obj.optString("word", ""),
                        contextSentence = obj.optString("contextSentence", ""),
                        bookTitle = obj.optString("bookTitle", ""),
                        note = obj.optString("note", ""),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun isWordSaved(word: String): Boolean {
        val clean = word.trim().lowercase()
        return getWords().any { it.word.trim().lowercase() == clean }
    }

    @Synchronized
    fun addWord(word: String, contextSentence: String, bookTitle: String, note: String = ""): VocabularyItem {
        val current = getWords().toMutableList()
        val cleanWord = word.trim()
        val existingIndex = current.indexOfFirst { it.word.equals(cleanWord, ignoreCase = true) }
        val item = VocabularyItem(
            id = if (existingIndex >= 0) current[existingIndex].id else System.currentTimeMillis(),
            word = cleanWord,
            contextSentence = contextSentence.trim(),
            bookTitle = bookTitle.trim(),
            note = note.trim(),
            timestamp = System.currentTimeMillis()
        )
        if (existingIndex >= 0) {
            current[existingIndex] = item
        } else {
            current.add(0, item)
        }
        saveList(current)
        return item
    }

    @Synchronized
    fun removeWord(id: Long): Boolean {
        val current = getWords().toMutableList()
        val removed = current.removeAll { it.id == id }
        if (removed) {
            saveList(current)
        }
        return removed
    }

    @Synchronized
    fun removeByWord(word: String): Boolean {
        val current = getWords().toMutableList()
        val clean = word.trim()
        val removed = current.removeAll { it.word.equals(clean, ignoreCase = true) }
        if (removed) {
            saveList(current)
        }
        return removed
    }

    @Synchronized
    fun clearAll() {
        prefs.edit().remove("saved_vocabularies").apply()
    }

    @Synchronized
    private fun saveList(list: List<VocabularyItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("word", item.word)
                put("contextSentence", item.contextSentence)
                put("bookTitle", item.bookTitle)
                put("note", item.note)
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString("saved_vocabularies", array.toString()).apply()
    }

    fun exportAsText(): String {
        val items = getWords()
        if (items.isEmpty()) return "生词本为空"
        val sb = StringBuilder()
        sb.append("=== 生词本导出 (共 ${items.size} 词) ===\n\n")
        items.forEachIndexed { index, item ->
            sb.append("${index + 1}. ${item.word}\n")
            if (item.contextSentence.isNotBlank()) {
                sb.append("   例句: ${item.contextSentence}\n")
            }
            if (item.bookTitle.isNotBlank()) {
                sb.append("   来源: 《${item.bookTitle}》\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    fun exportAsCsv(): String {
        val items = getWords()
        val sb = StringBuilder()
        sb.append("Word,Context,Book\n")
        items.forEach { item ->
            val w = "\"${item.word.replace("\"", "\"\"")}\""
            val c = "\"${item.contextSentence.replace("\"", "\"\"")}\""
            val b = "\"${item.bookTitle.replace("\"", "\"\"")}\""
            sb.append("$w,$c,$b\n")
        }
        return sb.toString()
    }
}
