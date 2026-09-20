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

    // English word numbers (Chapter One, Part Two, etc.)
    private const val EN_NUMBER_WORDS =
        "one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|" +
        "sixteen|seventeen|eighteen|nineteen|twenty|twenty-one|twenty-two|twenty-three|twenty-four|twenty-five|" +
        "twenty-six|twenty-seven|twenty-eight|twenty-nine|thirty|forty|fifty|sixty|seventy|eighty|ninety|hundred|" +
        "first|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth"

    // 1. English Chapter / Part / Book with numbers or word numbers:
    // "Chapter 1", "Chapter I", "Chapter One", "Part 2", "Book Three", "Section 4", "Volume 1", "Lesson 5", "Unit 3", "Episode 1"
    private val EN_CHAPTER_WITH_NUM_REGEX = Regex(
        """^\s*(Chapter|Section|Part|Book|Volume|Act|Scene|Lesson|Unit|Episode)\s+([0-9IVXLCDM]+|$EN_NUMBER_WORDS)\b(\s*[,:.\-—–]\s*.*|\s+.*)?$""",
        RegexOption.IGNORE_CASE
    )

    // 2. Standalone Roman numeral headings:
    // "I", "II.", "III - The Forest", "IV: The Journey"
    private val EN_ROMAN_NUMERAL_REGEX = Regex(
        """^\s*[IVXLCDM]{1,8}(\.|\s*[-–—:]\s*.*|\s+[A-Z].*)?$"""
    )

    // 3. Numbered English titles:
    // "1. Introduction", "2. The Boy Who Lived", "01 The Beginning"
    private val EN_NUMBERED_TITLE_REGEX = Regex(
        """^\s*\d{1,3}\.?\s+[A-Z][a-zA-Z0-9\s,'’"\-—:?]{2,50}$"""
    )

    // 4. Standard English structural headings:
    // Prologue, Epilogue, Preface, Foreword, Introduction, Afterword, Conclusion, Acknowledgements, Appendix A
    private val EN_STRUCTURAL_SECTIONS_REGEX = Regex(
        """^\s*(Prologue|Epilogue|Preface|Foreword|Introduction|Afterword|Conclusion|Acknowledgements?|Contents?|Dedication|Appendix(\s+[A-Z0-9]+)?)\b(\s*[:.\-—–]\s*.*|\s+.*)?$""",
        RegexOption.IGNORE_CASE
    )

    // Markdown headers: # Chapter 1, ## Section 2
    private val MD_HEADER_REGEX = Regex("""^#{1,4}\s+(.+)""")

    // Standalone ALL CAPS English title (e.g. "THE BOY WHO LIVED", "A DARK AND STORMY NIGHT")
    private val EN_ALL_CAPS_HEADING_REGEX = Regex("""^[A-Z0-9\s,'’"\-—:]{3,45}$""")

    fun isChapterTitle(rawText: String): Boolean {
        val trimmed = rawText.trim()
        if (trimmed.length !in 2..80) return false
        return CJK_CHAPTER_REGEX.matches(trimmed) ||
                EN_CHAPTER_WITH_NUM_REGEX.matches(trimmed) ||
                EN_ROMAN_NUMERAL_REGEX.matches(trimmed) ||
                EN_NUMBERED_TITLE_REGEX.matches(trimmed) ||
                EN_STRUCTURAL_SECTIONS_REGEX.matches(trimmed) ||
                MD_HEADER_REGEX.matches(trimmed) ||
                (trimmed.length in 4..40 &&
                        trimmed.count { it.isLetter() } >= 3 &&
                        EN_ALL_CAPS_HEADING_REGEX.matches(trimmed) &&
                        trimmed.none { it in 'a'..'z' } &&
                        !trimmed.endsWith(".") &&
                        !trimmed.endsWith(","))
    }

    fun detectChapters(sentences: List<String>): List<ChapterItem> {
        if (sentences.isEmpty()) return emptyList()

        val chapters = mutableListOf<ChapterItem>()

        sentences.forEachIndexed { index, sentence ->
            // Check the first line of the sentence in case the heading was grouped with following text
            val firstLine = sentence.lineSequence().firstOrNull()?.trim() ?: sentence.trim()
            val candidate = if (firstLine.length in 2..80) firstLine else sentence.trim()

            if (isChapterTitle(candidate)) {
                val cleanTitle = candidate
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
