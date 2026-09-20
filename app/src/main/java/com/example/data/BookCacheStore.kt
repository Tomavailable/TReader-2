package com.example.data

import android.content.Context
import android.util.Log
import android.util.LruCache
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

data class CachedBookStructure(
    val sentences: List<String>,
    val flowParagraphs: List<FlowParagraph>,
    val chapters: List<ChapterItem>
)

class BookCacheStore(private val context: Context) {
    private val booksDir: File by lazy {
        File(context.filesDir, "books").apply { if (!exists()) mkdirs() }
    }

    // In-memory LRU cache: key = "$bookId:$configHash"
    private val memoryCache = LruCache<String, CachedBookStructure>(4)

    fun getCache(bookId: String, configHash: Long): CachedBookStructure? {
        val memKey = "$bookId:$configHash"
        memoryCache.get(memKey)?.let { return it }

        val cacheFile = File(booksDir, "$bookId.cache")
        if (!cacheFile.exists() || cacheFile.length() == 0L) return null

        return try {
            DataInputStream(BufferedInputStream(FileInputStream(cacheFile), 65536)).use { dis ->
                val magic = dis.readInt()
                if (magic != MAGIC) return null
                val version = dis.readInt()
                if (version != VERSION) return null
                val savedHash = dis.readLong()
                if (savedHash != configHash) return null

                // 1. Sentences
                val sentencesCount = dis.readInt()
                val sentences = ArrayList<String>(sentencesCount)
                for (i in 0 until sentencesCount) {
                    sentences.add(readString(dis))
                }

                // 2. Chapters
                val chaptersCount = dis.readInt()
                val chapters = ArrayList<ChapterItem>(chaptersCount)
                for (i in 0 until chaptersCount) {
                    val id = dis.readInt()
                    val title = readString(dis)
                    val sentenceIndex = dis.readInt()
                    chapters.add(ChapterItem(id, title, sentenceIndex))
                }

                // 3. FlowParagraphs
                val paraCount = dis.readInt()
                val paragraphs = ArrayList<FlowParagraph>(paraCount)
                for (i in 0 until paraCount) {
                    val pIdx = dis.readInt()
                    val pRaw = readString(dis)
                    val spansCount = dis.readInt()
                    val spans = ArrayList<FlowSentenceSpan>(spansCount)
                    for (j in 0 until spansCount) {
                        val gIdx = dis.readInt()
                        val start = dis.readInt()
                        val end = dis.readInt()
                        val sText = readString(dis)
                        spans.add(FlowSentenceSpan(gIdx, start, end, sText))
                    }
                    paragraphs.add(FlowParagraph(pIdx, pRaw, spans))
                }

                val result = CachedBookStructure(sentences, paragraphs, chapters)
                memoryCache.put(memKey, result)
                result
            }
        } catch (e: Exception) {
            Log.w("BookCacheStore", "Failed to read cache for $bookId: ${e.message}")
            try { cacheFile.delete() } catch (_: Exception) {}
            null
        }
    }

    fun saveCache(bookId: String, configHash: Long, data: CachedBookStructure) {
        val memKey = "$bookId:$configHash"
        memoryCache.put(memKey, data)

        val tmpFile = File(booksDir, "$bookId.cache.tmp")
        val targetFile = File(booksDir, "$bookId.cache")

        try {
            DataOutputStream(BufferedOutputStream(FileOutputStream(tmpFile), 65536)).use { dos ->
                dos.writeInt(MAGIC)
                dos.writeInt(VERSION)
                dos.writeLong(configHash)

                // 1. Sentences
                dos.writeInt(data.sentences.size)
                for (s in data.sentences) {
                    writeString(dos, s)
                }

                // 2. Chapters
                dos.writeInt(data.chapters.size)
                for (c in data.chapters) {
                    dos.writeInt(c.id)
                    writeString(dos, c.title)
                    dos.writeInt(c.sentenceIndex)
                }

                // 3. FlowParagraphs
                dos.writeInt(data.flowParagraphs.size)
                for (p in data.flowParagraphs) {
                    dos.writeInt(p.paragraphIndex)
                    writeString(dos, p.rawText)
                    dos.writeInt(p.spans.size)
                    for (span in p.spans) {
                        dos.writeInt(span.globalSentenceIndex)
                        dos.writeInt(span.startInParagraph)
                        dos.writeInt(span.endInParagraph)
                        writeString(dos, span.sentenceText)
                    }
                }
                dos.flush()
            }

            if (tmpFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                tmpFile.renameTo(targetFile)
            }
        } catch (e: Exception) {
            Log.e("BookCacheStore", "Failed to write cache for $bookId: ${e.message}")
            try { tmpFile.delete() } catch (_: Exception) {}
        }
    }

    fun deleteCache(bookId: String) {
        memoryCache.remove(bookId)
        try {
            File(booksDir, "$bookId.cache").delete()
            File(booksDir, "$bookId.cache.tmp").delete()
        } catch (_: Exception) {}
    }

    private fun writeString(dos: DataOutputStream, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        dos.writeInt(bytes.size)
        dos.write(bytes)
    }

    private fun readString(dis: DataInputStream): String {
        val len = dis.readInt()
        val bytes = ByteArray(len)
        dis.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    companion object {
        private const val MAGIC = 0x54545343 // "TTSC"
        private const val VERSION = 1

        fun computeConfigHash(
            splitMode: SplitMode,
            isSplitEnabled: Boolean,
            secondaryPuncts: Set<Char>,
            terminatorPuncts: Set<Char>,
            closingPuncts: Set<Char>,
            simplePuncts: Set<Char>,
            abbreviations: Set<String>,
            secondarySplitMinLength: Int,
            secondarySplitScheme: SecondarySplitScheme
        ): Long {
            var result = splitMode.name.hashCode().toLong()
            result = 31 * result + if (isSplitEnabled) 1 else 0
            result = 31 * result + secondaryPuncts.hashCode()
            result = 31 * result + terminatorPuncts.hashCode()
            result = 31 * result + closingPuncts.hashCode()
            result = 31 * result + simplePuncts.hashCode()
            result = 31 * result + abbreviations.hashCode()
            result = 31 * result + secondarySplitMinLength
            result = 31 * result + secondarySplitScheme.name.hashCode()
            return result
        }
    }
}
