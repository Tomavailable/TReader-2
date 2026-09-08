package com.example.data

import java.text.BreakIterator
import java.util.Locale

data class FlowSentenceSpan(
    val globalSentenceIndex: Int,
    val startInParagraph: Int,
    val endInParagraph: Int,
    val sentenceText: String
)

data class FlowParagraph(
    val paragraphIndex: Int,
    val rawText: String,
    val spans: List<FlowSentenceSpan>
)

enum class SplitMode(val title: String, val shortDesc: String) {
    PARAGRAPH_FLOW("阅读", "保持文章原始段落排版·流式高亮朗读与即点即读"),
    SMART("整句1", "双指针状态机·缩写与浮点数精准避让"),
    SCHEME_A("整句2", "主流正则回溯与中英文行切分"),
    PUNCTUATION("自定义", "纯终止标点与闭合引号切分·极速无正则"),
    SUPER_SHORT("超短句", "默认依据逗号/分号进行二次智能拆分")
}

object TextSegmenter {

    val DEFAULT_SECONDARY_PUNCTS = setOf('，', ',', '；', ';', '：', ':', '、', '—', '–')
    val DEFAULT_TERMINATOR_PUNCTS = setOf('。', '？', '！', '…', '.', '?', '!')
    val DEFAULT_CLOSING_PUNCTS = setOf('”', '’', '"', '\'', '」', '』', '）', ')', '》', '>', '】', ']', '｝', '}')
    val DEFAULT_SIMPLE_PUNCTS = setOf('。', '？', '！', '…', '.', '?', '!')
    val DEFAULT_ABBREVIATIONS = setOf(
        "mr.", "mrs.", "ms.", "dr.", "prof.", "sr.", "jr.", "vs.", "etc.",
        "e.g.", "i.e.", "no.", "vol.", "jan.", "feb.", "mar.", "apr.",
        "aug.", "sept.", "oct.", "nov.", "dec.", "st.", "ave.", "rd.", "blvd.",
        "co.", "inc.", "corp.", "ltd.", "dept.", "est.", "approx."
    )

    /**
     * Splits raw text into sentences based on the chosen [splitMode], optional [isSplitEnabled],
     * and user-customizable punctuation rules.
     */
    fun splitIntoSentences(
        rawText: String,
        splitMode: SplitMode = SplitMode.SMART,
        isSplitEnabled: Boolean = false,
        secondaryPuncts: Set<Char> = DEFAULT_SECONDARY_PUNCTS,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS,
        simplePuncts: Set<Char> = DEFAULT_SIMPLE_PUNCTS,
        abbreviations: Set<String> = DEFAULT_ABBREVIATIONS,
        secondarySplitMinLength: Int = 30
    ): List<String> {
        if (rawText.isBlank()) return emptyList()

        val baseSentences = when (splitMode) {
            SplitMode.SMART, SplitMode.SUPER_SHORT -> splitSmart(rawText, terminatorPuncts, closingPuncts, abbreviations)
            SplitMode.PARAGRAPH_FLOW -> {
                val paragraphs = buildParagraphFlow(
                    rawText,
                    terminatorPuncts,
                    closingPuncts,
                    abbreviations,
                    isSplitEnabled,
                    secondaryPuncts,
                    secondarySplitMinLength
                )
                return paragraphs.flatMap { p -> p.spans.map { it.sentenceText } }
            }
            SplitMode.PUNCTUATION -> splitPunctuation(rawText, terminatorPuncts, closingPuncts)
            SplitMode.SCHEME_A -> splitWithRegexLookaround(rawText, terminatorPuncts, closingPuncts, abbreviations)
        }

        return if (isSplitEnabled || splitMode == SplitMode.SUPER_SHORT) {
            applySecondarySplitIfNeeded(baseSentences, secondaryPuncts, secondarySplitMinLength)
        } else {
            baseSentences
        }
    }

    /**
     * Builds structured paragraphs and sentence spans mapped to paragraph offsets for flow reading mode.
     */
    fun buildParagraphFlow(
        rawText: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS,
        abbreviations: Set<String> = DEFAULT_ABBREVIATIONS,
        isSplitEnabled: Boolean = false,
        secondaryPuncts: Set<Char> = DEFAULT_SECONDARY_PUNCTS,
        secondarySplitMinLength: Int = 30
    ): List<FlowParagraph> {
        if (rawText.isBlank()) return emptyList()

        val rawParagraphs = rawText.split(Regex("\r?\n+"))
        val result = mutableListOf<FlowParagraph>()
        var globalIndex = 0

        rawParagraphs.forEachIndexed { pIdx, pText ->
            val trimmed = pText.trim()
            if (trimmed.isNotEmpty()) {
                val baseSentences = splitSmart(trimmed, terminatorPuncts, closingPuncts, abbreviations)
                val finalSentences = if (isSplitEnabled) {
                    applySecondarySplitIfNeeded(baseSentences, secondaryPuncts, secondarySplitMinLength)
                } else {
                    baseSentences
                }

                val spans = mutableListOf<FlowSentenceSpan>()
                var searchOffset = 0
                for (sentence in finalSentences) {
                    val cleanSentence = sentence.trim()
                    if (cleanSentence.isEmpty()) continue

                    val foundIndex = trimmed.indexOf(cleanSentence, searchOffset)
                    val start = if (foundIndex != -1) foundIndex else searchOffset
                    val end = (start + cleanSentence.length).coerceAtMost(trimmed.length)
                    searchOffset = end

                    spans.add(
                        FlowSentenceSpan(
                            globalSentenceIndex = globalIndex,
                            startInParagraph = start,
                            endInParagraph = end,
                            sentenceText = cleanSentence
                        )
                    )
                    globalIndex++
                }

                result.add(
                    FlowParagraph(
                        paragraphIndex = pIdx,
                        rawText = trimmed,
                        spans = spans
                    )
                )
            }
        }
        return result
    }

    /**
     * Overload for backwards compatibility
     */
    fun splitIntoSentences(
        rawText: String,
        splitComma: Boolean,
        splitMode: SplitMode = SplitMode.SCHEME_A
    ): List<String> {
        return splitIntoSentences(rawText, splitMode, isSplitEnabled = splitComma)
    }

    /**
     * Secondary splitting on long sentences when [isSplitEnabled] is true.
     */
    private fun applySecondarySplitIfNeeded(
        sentences: List<String>,
        secondaryPuncts: Set<Char> = DEFAULT_SECONDARY_PUNCTS,
        minLength: Int = 30
    ): List<String> {
        if (sentences.isEmpty()) return sentences

        val result = mutableListOf<String>()
        for (sentence in sentences) {
            val subSegments = splitSingleSentenceByLength(sentence, secondaryPuncts, minLength)
            result.addAll(subSegments)
        }
        return result
    }

    private fun splitSingleSentenceByLength(
        sentence: String,
        subPuncts: Set<Char> = DEFAULT_SECONDARY_PUNCTS,
        minLength: Int = 40 // Changed from 30
    ): List<String> {
        val len = sentence.length
        if (len <= minLength) return listOf(sentence)

        val punctIndices = mutableListOf<Int>()
        for (i in 0 until len) {
            if (subPuncts.contains(sentence[i])) {
                punctIndices.add(i)
            }
        }

        if (punctIndices.isEmpty()) {
            return listOf(sentence)
        }

        // Changed thresholds from 80/150 to 90
        if (len <= 90) {
            val mid = len / 2
            val bestIdx = punctIndices.minByOrNull { kotlin.math.abs(it - mid) } ?: return listOf(sentence)
            val part1 = cleanAndFormatSentence(sentence.substring(0, bestIdx + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(bestIdx + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        // > 90 字符：根据标点拆分为 2 个或最多拆分为 3 个句子。
        val target1 = len / 3
        val target2 = (len * 2) / 3

        if (punctIndices.size == 1) {
            val idx = punctIndices[0]
            val part1 = cleanAndFormatSentence(sentence.substring(0, idx + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(idx + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        var best1 = punctIndices.minByOrNull { kotlin.math.abs(it - target1) }!!
        var best2 = punctIndices.minByOrNull { kotlin.math.abs(it - target2) }!!

        if (best1 == best2) {
            val otherFor2 = punctIndices.filter { it != best1 }.minByOrNull { kotlin.math.abs(it - target2) }
            if (otherFor2 != null) {
                best2 = otherFor2
            }
        }

        if (best1 > best2) {
            val temp = best1
            best1 = best2
            best2 = temp
        }

        if (best1 == best2) {
            val part1 = cleanAndFormatSentence(sentence.substring(0, best1 + 1))
            val part2 = cleanAndFormatSentence(sentence.substring(best1 + 1))
            val res = mutableListOf<String>()
            if (part1.isNotEmpty()) res.add(part1)
            if (part2.isNotEmpty()) res.add(part2)
            return if (res.isEmpty()) listOf(sentence) else res
        }

        val part1 = cleanAndFormatSentence(sentence.substring(0, best1 + 1))
        val part2 = cleanAndFormatSentence(sentence.substring(best1 + 1, best2 + 1))
        val part3 = cleanAndFormatSentence(sentence.substring(best2 + 1))

        val res = mutableListOf<String>()
        if (part1.isNotEmpty()) res.add(part1)
        if (part2.isNotEmpty()) res.add(part2)
        if (part3.isNotEmpty()) res.add(part3)

        return if (res.isEmpty()) listOf(sentence) else res
    }

    /**
     * Removes internal newlines and formats spacing so the sentence renders continuously
     */
    private fun cleanAndFormatSentence(sentence: String): String {
        var cleaned = sentence.replace(Regex("(?<=[\u4e00-\u9fa5])\r?\n(?=[\u4e00-\u9fa5])"), "")
        cleaned = cleaned.replace(Regex("[\r\n]+"), " ")
        cleaned = cleaned.replace(Regex("[ \\t]+"), " ")
        return cleaned.trim()
    }

    /**
     * 方案 1: 极速双指针状态机智能断句 (Smart)
     * - 正则与状态机向前/向后扫描
     * - 精准避让缩写词 (Mr., Dr., e.g., etc.) 与数值 (3.14)
     * - 完美吸附闭合引号/括号
     * - 无论句号后是空格、换行还是紧贴大写，均能准确断句
     */
    fun splitSmart(
        rawText: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS,
        abbreviations: Set<String> = DEFAULT_ABBREVIATIONS
    ): List<String> {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
        val paragraphs = normalized.split(Regex("\n+")).filter { it.isNotBlank() }
        val effectiveTerminators = if (terminatorPuncts.isNotEmpty()) terminatorPuncts else DEFAULT_TERMINATOR_PUNCTS

        val normAbbr = mutableSetOf<String>()
        for (a in abbreviations) {
            val cl = a.trim().lowercase(Locale.ROOT)
            if (cl.isNotEmpty()) {
                normAbbr.add(cl)
                normAbbr.add(cl.removeSuffix("."))
                normAbbr.add(cl.removeSuffix(".") + ".")
            }
        }

        val result = mutableListOf<String>()

        for (paragraph in paragraphs) {
            val text = paragraph.trim()
            if (text.isEmpty()) continue

            val len = text.length
            var start = 0
            var i = 0

            while (i < len) {
                val c = text[i]

                if (effectiveTerminators.contains(c)) {
                    // 1. 检查是否为浮点数 (如 3.14)
                    if (c == '.' && i > 0 && i < len - 1 && text[i - 1].isDigit() && text[i + 1].isDigit()) {
                        i++
                        continue
                    }

                    // 2. 检查是否为已知缩写词 (如 Mr., Dr., e.g.)
                    if (c == '.') {
                        var wordStart = i - 1
                        while (wordStart >= 0 && (text[wordStart].isLetter() || text[wordStart] == '.')) {
                            wordStart--
                        }
                        val wordWithDot = text.substring(wordStart + 1, i + 1).lowercase(Locale.ROOT)
                        if (normAbbr.contains(wordWithDot) || normAbbr.contains(wordWithDot.removeSuffix("."))) {
                            i++
                            continue
                        }
                    }

                    // 3. 吸附后续的闭合标点 (如 ." 或 ！”)
                    var end = i + 1
                    while (end < len && closingPuncts.contains(text[end])) {
                        end++
                    }

                    // 截取句子
                    val segment = text.substring(start, end).trim()
                    val cleaned = cleanAndFormatSentence(segment)
                    if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, effectiveTerminators, closingPuncts)) {
                        result.add(cleaned)
                    }

                    // 跳过后续空白
                    while (end < len && (text[end] == ' ' || text[end] == '\t')) {
                        end++
                    }
                    start = end
                    i = end
                    continue
                }
                i++
            }

            if (start < len) {
                val remaining = text.substring(start).trim()
                val cleaned = cleanAndFormatSentence(remaining)
                if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, effectiveTerminators, closingPuncts)) {
                    result.add(cleaned)
                }
            }
        }

        return if (result.isEmpty()) splitByLines(rawText) else result
    }

    /**
     * 方案 2: 标准标点极简断句 (Punctuation)
     * - 纯粹按终止标点和闭合符号切分，无缩写词匹配开销
     */
    fun splitPunctuation(
        rawText: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): List<String> {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
        val paragraphs = normalized.split(Regex("\n+")).filter { it.isNotBlank() }
        val effectiveTerminators = if (terminatorPuncts.isNotEmpty()) terminatorPuncts else DEFAULT_TERMINATOR_PUNCTS

        val result = mutableListOf<String>()

        for (paragraph in paragraphs) {
            val text = paragraph.trim()
            if (text.isEmpty()) continue

            val len = text.length
            var start = 0
            var i = 0

            while (i < len) {
                val c = text[i]
                if (effectiveTerminators.contains(c)) {
                    var end = i + 1
                    while (end < len && closingPuncts.contains(text[end])) {
                        end++
                    }
                    val segment = text.substring(start, end).trim()
                    val cleaned = cleanAndFormatSentence(segment)
                    if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, effectiveTerminators, closingPuncts)) {
                        result.add(cleaned)
                    }
                    while (end < len && (text[end] == ' ' || text[end] == '\t')) {
                        end++
                    }
                    start = end
                    i = end
                    continue
                }
                i++
            }

            if (start < len) {
                val remaining = text.substring(start).trim()
                val cleaned = cleanAndFormatSentence(remaining)
                if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, effectiveTerminators, closingPuncts)) {
                    result.add(cleaned)
                }
            }
        }

        return if (result.isEmpty()) splitByLines(rawText) else result
    }

    /**
     * 方案 3: 国际 ICU BreakIterator 标准断句 (ICU)
     * - 基于 Android / Java 标准库国际语言学分句引擎
     */
    fun splitIcu(
        rawText: String,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): List<String> {
        val normalized = rawText.replace("\r\n", "\n").replace("\r", "\n")
        val paragraphs = normalized.split(Regex("\n+")).filter { it.isNotBlank() }
        val result = mutableListOf<String>()

        val iterator = BreakIterator.getSentenceInstance(Locale.getDefault())

        for (paragraph in paragraphs) {
            val text = paragraph.trim()
            if (text.isEmpty()) continue

            iterator.setText(text)
            var start = iterator.first()
            var end = iterator.next()

            while (end != BreakIterator.DONE) {
                var actualEnd = end
                while (actualEnd < text.length && closingPuncts.contains(text[actualEnd])) {
                    actualEnd++
                }
                val segment = text.substring(start, actualEnd).trim()
                val cleaned = cleanAndFormatSentence(segment)
                if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, DEFAULT_TERMINATOR_PUNCTS, closingPuncts)) {
                    result.add(cleaned)
                }
                start = actualEnd
                if (end < actualEnd) {
                    iterator.following(actualEnd)
                }
                end = iterator.next()
            }
        }

        return if (result.isEmpty()) splitByLines(rawText) else result
    }

    /**
     * Strategy 1: Standard E-reader Lookaround & Deterministic Sentence Segmentation.
     * Accurately splits on sentence terminators (CJK and Western) followed by closing quotes,
     * while protecting decimals, abbreviations (Mr., Dr., etc.), and preserving line breaks.
     */
    private fun splitWithRegexLookaround(
        rawText: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS,
        abbreviations: Set<String> = DEFAULT_ABBREVIATIONS
    ): List<String> {
        return try {
            val lines = rawText.split(Regex("[\r\n]+"))
            val result = mutableListOf<String>()

            for (line in lines) {
                val trimmedLine = line.trim()
                if (trimmedLine.isEmpty()) continue

                val sentencesInLine = segmentSingleLine(
                    line = trimmedLine,
                    terminatorPuncts = if (terminatorPuncts.isNotEmpty()) terminatorPuncts else DEFAULT_TERMINATOR_PUNCTS,
                    closingPuncts = closingPuncts,
                    abbreviations = abbreviations
                )
                result.addAll(sentencesInLine)
            }

            if (result.isEmpty()) splitByLines(rawText) else result
        } catch (e: Exception) {
            splitByLines(rawText)
        }
    }

    /**
     * Strategy 2: Long Sentence Parser (Scheme C).
     * Smooths intra-paragraph soft line breaks first, then segments at major sentence terminators.
     */
    private fun splitWithLongSentenceParser(
        rawText: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS,
        abbreviations: Set<String> = DEFAULT_ABBREVIATIONS
    ): List<String> {
        return try {
            val paragraphs = rawText.split(Regex("(\r?\n){2,}"))
            val result = mutableListOf<String>()

            for (p in paragraphs) {
                val trimmedP = p.trim()
                if (trimmedP.isEmpty()) continue

                val smoothed = cleanAndFormatSentence(trimmedP)
                val sentencesInParagraph = segmentSingleLine(
                    line = smoothed,
                    terminatorPuncts = if (terminatorPuncts.isNotEmpty()) terminatorPuncts else DEFAULT_TERMINATOR_PUNCTS,
                    closingPuncts = closingPuncts,
                    abbreviations = abbreviations
                )
                result.addAll(sentencesInParagraph)
            }

            if (result.isEmpty()) splitByLines(rawText) else result
        } catch (e: Exception) {
            splitByLines(rawText)
        }
    }

    /**
     * Core deterministic sentence segmenter that handles:
     * 1. CJK and English terminators (。！？…!?)
     * 2. Multiple closing quotes / brackets (”’"')}】)
     * 3. Decimal protection (3.14)
     * 4. Abbreviation protection (Mr., Dr., etc.)
     */
    private fun segmentSingleLine(
        line: String,
        terminatorPuncts: Set<Char>,
        closingPuncts: Set<Char>,
        abbreviations: Set<String>
    ): List<String> {
        if (line.isBlank()) return emptyList()

        // Protect decimals (3.14) and abbreviations
        var protectedText = line
        val placeholderMap = mutableMapOf<String, String>()
        var placeholderCounter = 0

        val decimalRegex = Regex("(?<=\\d)\\.(?=\\d)")
        protectedText = decimalRegex.replace(protectedText) {
            val ph = "___DEC_${placeholderCounter++}___"
            placeholderMap[ph] = "."
            ph
        }

        for (abbr in abbreviations) {
            val cleanAbbr = abbr.trim()
            if (cleanAbbr.isEmpty()) continue
            val escapedAbbr = Regex.escape(cleanAbbr.removeSuffix("."))
            val abbrRegex = Regex("(?i)\\b$escapedAbbr\\.")
            protectedText = abbrRegex.replace(protectedText) { match ->
                val ph = "___ABBR_${placeholderCounter++}___"
                placeholderMap[ph] = match.value
                ph
            }
        }

        // Single letter abbreviation e.g. "A. Smith"
        val singleLetterRegex = Regex("(?i)\\b[a-z]\\.(?=\\s+[a-z])")
        protectedText = singleLetterRegex.replace(protectedText) { match ->
            val ph = "___INIT_${placeholderCounter++}___"
            placeholderMap[ph] = match.value
            ph
        }

        val sentences = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        val len = protectedText.length

        while (i < len) {
            val ch = protectedText[i]
            current.append(ch)

            if (terminatorPuncts.contains(ch)) {
                // Check if '.' is inside a domain/url like google.com
                var isRealEnd = true
                if (ch == '.') {
                    if (i + 1 < len && protectedText[i + 1].isLetterOrDigit()) {
                        isRealEnd = false
                    }
                }

                if (isRealEnd) {
                    // Absorb any following closing quotes / brackets
                    while (i + 1 < len && closingPuncts.contains(protectedText[i + 1])) {
                        i++
                        current.append(protectedText[i])
                    }

                    var rawSentence = current.toString()
                    placeholderMap.forEach { (ph, original) ->
                        rawSentence = rawSentence.replace(ph, original)
                    }
                    val cleaned = cleanAndFormatSentence(rawSentence)
                    if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, terminatorPuncts, closingPuncts)) {
                        sentences.add(cleaned)
                    }
                    current.clear()
                }
            }
            i++
        }

        if (current.isNotBlank()) {
            var rawSentence = current.toString()
            placeholderMap.forEach { (ph, original) ->
                rawSentence = rawSentence.replace(ph, original)
            }
            val cleaned = cleanAndFormatSentence(rawSentence)
            if (cleaned.isNotEmpty() && !isOnlyPunctuationOrWhitespace(cleaned, terminatorPuncts, closingPuncts)) {
                sentences.add(cleaned)
            }
        }

        return sentences
    }

    /**
     * Strategy 3: Simple punctuation-based splitting
     */
    private fun splitWithSimpleParser(
        rawText: String,
        simplePuncts: Set<Char> = DEFAULT_SIMPLE_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): List<String> {
        val paragraphs = rawText.split(Regex("(\r?\n){2,}"))
        val result = mutableListOf<String>()

        for (p in paragraphs) {
            val trimmedP = p.trim()
            if (trimmedP.isEmpty()) continue

            val text = cleanAndFormatSentence(trimmedP)
            val current = StringBuilder()
            var i = 0
            val len = text.length

            while (i < len) {
                val ch = text[i]
                current.append(ch)

                if (simplePuncts.contains(ch)) {
                    // Include any trailing closing quotes/brackets
                    while (i + 1 < len && closingPuncts.contains(text[i + 1])) {
                        i++
                        current.append(text[i])
                    }
                    val s = cleanAndFormatSentence(current.toString())
                    if (s.isNotEmpty() && !isOnlyPunctuationOrWhitespace(s, simplePuncts, closingPuncts)) {
                        result.add(s)
                    }
                    current.clear()
                }
                i++
            }

            if (current.isNotBlank()) {
                val s = cleanAndFormatSentence(current.toString())
                if (s.isNotEmpty() && !isOnlyPunctuationOrWhitespace(s, simplePuncts, closingPuncts)) {
                    result.add(s)
                }
            }
        }

        return if (result.isEmpty()) splitByLines(rawText) else result
    }

    private fun shouldMergeWithNext(
        currentChunk: String,
        nextChunk: String,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS,
        abbreviations: Set<String> = DEFAULT_ABBREVIATIONS
    ): Boolean {
        if (currentChunk.isEmpty() || nextChunk.isEmpty()) return false

        val nextFirstChar = nextChunk.trimStart().firstOrNull()
        if (nextFirstChar != null && closingPuncts.contains(nextFirstChar)) {
            return true
        }

        val trimmedCurrent = currentChunk.trimEnd()
        if (!trimmedCurrent.endsWith('.')) return false

        val lastChar = trimmedCurrent.dropLast(1).lastOrNull()
        val firstChar = nextChunk.trimStart().firstOrNull()

        if (lastChar != null && lastChar.isDigit() && firstChar != null && firstChar.isDigit()) {
            return true
        }

        if (lastChar != null && (lastChar.isLetterOrDigit() || lastChar == '-') && firstChar != null && firstChar.isLetter()) {
            if (!currentChunk.endsWith(" ") && !nextChunk.startsWith(" ")) {
                return true
            }
        }

        val prevText = trimmedCurrent.dropLast(1).trimEnd()
        val lastWord = prevText.substringAfterLast(' ').lowercase()
        if (abbreviations.contains(lastWord) || abbreviations.contains(lastWord.removePrefix("."))) {
            return true
        }
        if (lastWord.length == 1 && lastWord[0].isUpperCase()) {
            return true
        }

        return false
    }

    private fun splitByLines(rawText: String): List<String> {
        return rawText.lines().map { cleanAndFormatSentence(it) }.filter {
            it.isNotEmpty() && !isOnlyPunctuationOrWhitespace(it)
        }
    }

    private fun isOnlyPunctuationOrWhitespace(
        text: String,
        terminatorPuncts: Set<Char> = DEFAULT_TERMINATOR_PUNCTS,
        closingPuncts: Set<Char> = DEFAULT_CLOSING_PUNCTS
    ): Boolean {
        val allSymbols = terminatorPuncts + closingPuncts + DEFAULT_SECONDARY_PUNCTS
        return text.all { it.isWhitespace() || allSymbols.contains(it) }
    }
}
