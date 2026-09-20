package com.example.data

data class ChapterItem(
    val id: Int,
    val title: String,
    val sentenceIndex: Int
)

object ChapterDetector {

    // Common Chinese chapter patterns:
    // 第1章 ..., 第一百二十章 ..., 卷一 ..., 序言, 前言, 尾声, 结语, 番外 ...
    private val CJK_CHAPTER_REGEX = Regex(
        """^\s*(第\s*[0-9一二三四五六七八九十百千万零〇两]+\s*[章回节卷集幕篇部话]|引子|序言|序|前言|自序|楔子|尾声|后记|结语|终章|番外(\s*\d+)?)\s*.*"""
    )

    // Common Western / English chapter patterns:
    // Chapter 1 ..., Part I ..., Book 2 ..., Section 3 ...
    private val EN_CHAPTER_REGEX = Regex(
        """^\s*(Chapter|Section|Part|Book|Volume|Act|Scene)\s+[0-9IVXLCDM]+.*""",
        RegexOption.IGNORE_CASE
    )

    // Markdown headers: # Chapter 1, ## Section 2
    private val MD_HEADER_REGEX = Regex("""^#{1,3}\s+(.+)""")

    fun detectChapters(sentences: List<String>): List<ChapterItem> {
        if (sentences.isEmpty()) return emptyList()

        val chapters = mutableListOf<ChapterItem>()

        sentences.forEachIndexed { index, sentence ->
            val trimmed = sentence.trim()
            // Reasonable length for a chapter heading (between 2 and 60 chars)
            if (trimmed.length in 2..60) {
                val isChapter = CJK_CHAPTER_REGEX.matches(trimmed) ||
                        EN_CHAPTER_REGEX.matches(trimmed) ||
                        MD_HEADER_REGEX.matches(trimmed)

                if (isChapter) {
                    val cleanTitle = trimmed
                        .removePrefix("#").removePrefix("#").removePrefix("#")
                        .trim()
                    chapters.add(
                        ChapterItem(
                            id = chapters.size + 1,
                            title = cleanTitle,
                            sentenceIndex = index
                        )
                    )
                }
            }
        }

        // If no chapters matched or very few, and text is long, generate logical partitions so user can still navigate
        if (chapters.isEmpty() && sentences.size > 50) {
            val chunkSize = when {
                sentences.size > 1500 -> 100
                sentences.size > 400 -> 50
                else -> 30
            }
            var part = 1
            for (i in sentences.indices step chunkSize) {
                val preview = sentences[i].take(18).trim().replace("\n", " ")
                chapters.add(
                    ChapterItem(
                        id = part,
                        title = "第 $part 节: ${if (preview.isNotEmpty()) "$preview..." else ""}",
                        sentenceIndex = i
                    )
                )
                part++
            }
        } else if (chapters.isEmpty()) {
            chapters.add(
                ChapterItem(
                    id = 1,
                    title = "正文 (全文)",
                    sentenceIndex = 0
                )
            )
        }

        return chapters
    }
}
