package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

object DocumentParser {

    data class ParsedDocument(
        val title: String,
        val text: String,
        val format: String
    )

    fun getFileName(context: Context, uri: Uri): String {
        var fileName = "导入文档.txt"
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        val name = it.getString(nameIndex)
                        if (!name.isNullOrBlank()) fileName = name
                    }
                }
            }
        } catch (e: Exception) {
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrBlank()) {
                fileName = lastSegment.substringAfterLast('/')
            }
        }
        return fileName
    }

    /**
     * Parses TXT, SRT, VTT, LRC, EPUB, PDF, DOCX directly from Android Uri
     */
    fun parseUri(context: Context, uri: Uri): ParsedDocument {
        val fileName = getFileName(context, uri)
        val ext = fileName.substringAfterLast('.', "").lowercase()

        if (ext == "pdf") {
            val pdfText = parsePdf(context, uri)
            return ParsedDocument(fileName, pdfText, "PDF")
        }

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bytes = inputStream.readBytes()
            return when (ext) {
                "docx" -> {
                    val docxText = parseDocx(bytes)
                    ParsedDocument(fileName, docxText, "DOCX")
                }
                "epub" -> {
                    val epubText = parseEpub(bytes)
                    ParsedDocument(fileName, epubText, "EPUB")
                }
                "srt" -> {
                    val srtText = parseSrt(decodeBytes(bytes))
                    ParsedDocument(fileName, srtText, "SRT")
                }
                "vtt" -> {
                    val vttText = parseVtt(decodeBytes(bytes))
                    ParsedDocument(fileName, vttText, "VTT")
                }
                "lrc" -> {
                    val lrcText = parseLrc(decodeBytes(bytes))
                    ParsedDocument(fileName, lrcText, "LRC")
                }
                else -> {
                    // Default plain text (TXT, MD, etc.)
                    val txt = decodeBytes(bytes)
                    ParsedDocument(fileName, txt, "TXT")
                }
            }
        }

        throw IllegalArgumentException("无法读取该文件内容")
    }

    /**
     * Parse PDF document using PDFBox-Android with layout sorting
     */
    fun parsePdf(context: Context, uri: Uri): String {
        try {
            PDFBoxResourceLoader.init(context.applicationContext)
        } catch (_: Exception) {}

        val rawText = try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                PDDocument.load(inputStream).use { document ->
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true // Layout-aware multi-column extraction
                    stripper.getText(document)
                }
            } ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }

        return cleanPdfText(rawText)
    }

    /**
     * Cleans up PDF hard line breaks, hyphenated line wraps, and formats paragraphs smoothly
     */
    fun cleanPdfText(raw: String): String {
        if (raw.isBlank()) return ""

        // 1. Normalize line endings
        var text = raw.replace("\r\n", "\n").replace("\r", "\n")

        // 2. Fix hyphenated words across lines (e.g. "com-\nplementary" -> "complementary")
        text = text.replace(Regex("""([a-zA-Z]+)-\n([a-zA-Z]+)""")) { match ->
            match.groupValues[1] + match.groupValues[2]
        }

        // 3. Process line breaks: preserve double newlines (\n\n) as paragraph breaks,
        // and stitch single newlines (\n) within a paragraph.
        val paragraphs = text.split(Regex("""\n{2,}"""))
        val cleanedParagraphs = paragraphs.map { para ->
            val lines = para.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return@map ""

            val sb = StringBuilder()
            for (line in lines) {
                if (sb.isEmpty()) {
                    sb.append(line)
                } else {
                    val prevChar = sb.last()
                    val nextChar = line.first()

                    val isPrevCjk = isCjkChar(prevChar)
                    val isNextCjk = isCjkChar(nextChar)

                    if (isPrevCjk && isNextCjk) {
                        // Chinese/Japanese/Korean text: connect directly without space
                        sb.append(line)
                    } else if (isPrevCjk || isNextCjk) {
                        sb.append(line)
                    } else {
                        // Western text: connect with a space
                        sb.append(" ").append(line)
                    }
                }
            }
            sb.toString()
        }

        return cleanedParagraphs.filter { it.isNotBlank() }.joinToString("\n\n")
    }

    private fun isCjkChar(c: Char): Boolean {
        if (Character.isIdeographic(c.code)) return true
        val ub = Character.UnicodeBlock.of(c)
        return ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
               ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
               ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
               ub == Character.UnicodeBlock.HIRAGANA ||
               ub == Character.UnicodeBlock.KATAKANA ||
               ub == Character.UnicodeBlock.HANGUL_SYLLABLES
    }

    fun parsePartialTxt(inputStream: InputStream, maxLines: Int): String {
        val reader = inputStream.bufferedReader()
        val result = StringBuilder()
        var lineCount = 0
        reader.forEachLine { line ->
            if (lineCount >= maxLines) return@forEachLine
            result.append(line).append("\n")
            lineCount++
        }
        return result.toString().trim()
    }

    /**
     * Robust charset decoding with UTF-8 and GBK fallback
     */
    private fun decodeBytes(bytes: ByteArray): String {
        var text = String(bytes, Charsets.UTF_8)
        if (text.contains("\uFFFD")) {
            try {
                val gbkText = String(bytes, Charset.forName("GBK"))
                if (!gbkText.contains("\uFFFD")) {
                    text = gbkText
                }
            } catch (_: Exception) {}
        }
        return text
    }

    /**
     * Parses SubRip (.srt) subtitle files:
     * Strips index numbers, timestamps (00:01:20,000 --> 00:01:23,000), HTML tags,
     * and stitches subtitle lines smoothly until a sentence terminator is reached.
     */
    fun parseSrt(content: String): String {
        val lines = content.lines()
        val extractedLines = mutableListOf<String>()
        var lastLine = ""

        val timestampRegex = Regex("""^\d{1,2}:\d{2}:\d{2}[,\.]\d{3}\s*-->\s*\d{1,2}:\d{2}:\d{2}[,\.]\d{3}""")
        val htmlTagRegex = Regex("""<[^>]+>""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            // Skip purely numeric index lines
            if (trimmed.all { it.isDigit() }) continue
            // Skip timestamp lines
            if (timestampRegex.containsMatchIn(trimmed)) continue
            if (trimmed.contains("-->")) continue

            // Strip HTML/formatting tags like <i>, <b>, <font color="...">
            val clean = trimmed.replace(htmlTagRegex, "").trim()
            if (clean.isNotEmpty() && clean != lastLine) {
                extractedLines.add(clean)
                lastLine = clean
            }
        }
        return stitchSubtitleLines(extractedLines)
    }

    /**
     * Parses WebVTT (.vtt) subtitle files
     */
    fun parseVtt(content: String): String {
        val lines = content.lines()
        val extractedLines = mutableListOf<String>()
        val timestampRegex = Regex("""(?:\d{2}:)?\d{2}:\d{2}\.\d{3}\s*-->\s*(?:\d{2}:)?\d{2}:\d{2}\.\d{3}""")
        val htmlTagRegex = Regex("""<[^>]+>""")
        var lastLine = ""

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("WEBVTT") || trimmed.startsWith("NOTE") || trimmed.startsWith("STYLE")) continue
            if (trimmed.all { it.isDigit() }) continue
            if (timestampRegex.containsMatchIn(trimmed)) continue
            if (trimmed.contains("-->")) continue

            val clean = trimmed.replace(htmlTagRegex, "").trim()
            if (clean.isNotEmpty() && clean != lastLine) {
                extractedLines.add(clean)
                lastLine = clean
            }
        }
        return stitchSubtitleLines(extractedLines)
    }

    /**
     * Parses Lyric (.lrc) files by stripping [mm:ss.xx] timestamps
     */
    fun parseLrc(content: String): String {
        val lines = content.lines()
        val extractedLines = mutableListOf<String>()
        val lrcRegex = Regex("""\[\d{2}:\d{2}(?:\.\d{2,3})?\]""")

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            // Skip header tags like [ti:Title], [ar:Artist], etc.
            if (trimmed.matches(Regex("""^\[[a-zA-Z]+:.*\]$"""))) continue

            val clean = trimmed.replace(lrcRegex, "").trim()
            if (clean.isNotEmpty()) {
                extractedLines.add(clean)
            }
        }
        return stitchSubtitleLines(extractedLines)
    }

    /**
     * Stitches fragmented subtitle lines into continuous paragraphs/sentences
     */
    private fun stitchSubtitleLines(lines: List<String>): String {
        if (lines.isEmpty()) return ""

        val terminators = setOf('。', '？', '！', '…', '.', '?', '!', ';', '；')
        val paragraphs = mutableListOf<String>()
        val currentPara = StringBuilder()

        for (line in lines) {
            if (currentPara.isEmpty()) {
                currentPara.append(line)
            } else {
                val lastChar = currentPara.trimEnd().last()
                if (terminators.contains(lastChar)) {
                    // Previous sentence ended with punctuation, commit paragraph
                    paragraphs.add(currentPara.toString().trim())
                    currentPara.clear()
                    currentPara.append(line)
                } else {
                    // Sentence was cut across subtitle frames, stitch smoothly
                    val nextFirstChar = line.first()
                    val isPrevCjk = isCjkChar(lastChar)
                    val isNextCjk = isCjkChar(nextFirstChar)

                    if (isPrevCjk && isNextCjk) {
                        currentPara.append(line)
                    } else if (isPrevCjk || isNextCjk) {
                        currentPara.append(line)
                    } else {
                        currentPara.append(" ").append(line)
                    }
                }
            }
        }

        if (currentPara.isNotEmpty()) {
            paragraphs.add(currentPara.toString().trim())
        }

        return paragraphs.filter { it.isNotBlank() }.joinToString("\n\n")
    }

    /**
     * Lightweight zero-dependency DOCX Parser:
     * Reads word/document.xml from the ZIP container and extracts paragraph text
     */
    fun parseDocx(bytes: ByteArray): String {
        var docXmlBytes: ByteArray? = null
        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        docXmlBytes = zis.readBytes()
                        break
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

        if (docXmlBytes == null) return ""

        val result = StringBuilder()
        val currentPara = StringBuilder()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(docXmlBytes), "UTF-8")

            var eventType = parser.eventType
            var inTextTag = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name.lowercase()
                        if (name == "t") {
                            inTextTag = true
                        } else if (name == "br" || name == "cr") {
                            currentPara.append("\n")
                        } else if (name == "tab") {
                            currentPara.append(" ")
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inTextTag) {
                            currentPara.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name.lowercase()
                        if (name == "t") {
                            inTextTag = false
                        } else if (name == "p") {
                            val paraText = currentPara.toString().trim()
                            if (paraText.isNotEmpty()) {
                                result.append(paraText).append("\n\n")
                            }
                            currentPara.clear()
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val text = result.toString().trim()
        if (text.isNotEmpty()) return text

        return extractDocxRegexFallback(String(docXmlBytes!!, Charsets.UTF_8))
    }

    private fun extractDocxRegexFallback(xml: String): String {
        val paraRegex = Regex("""<w:p(?:\s[^>]*)?>([\s\S]*?)</w:p>""", RegexOption.IGNORE_CASE)
        val textRegex = Regex("""<w:t(?:\s[^>]*)?>([\s\S]*?)</w:t>""", RegexOption.IGNORE_CASE)

        val result = StringBuilder()
        for (paraMatch in paraRegex.findAll(xml)) {
            val paraContent = paraMatch.groupValues[1]
            val paraText = StringBuilder()
            for (textMatch in textRegex.findAll(paraContent)) {
                paraText.append(textMatch.groupValues[1])
            }
            val clean = unescapeHtml(paraText.toString()).trim()
            if (clean.isNotEmpty()) {
                result.append(clean).append("\n\n")
            }
        }
        return result.toString().trim()
    }

    /**
     * Lightweight zero-dependency EPUB Parser:
     * 1. Finds OPF manifest path in META-INF/container.xml
     * 2. Reads spine item order
     * 3. Extracts text from XHTML/HTML chapters stripping markup
     */
    fun parseEpub(bytes: ByteArray): String {
        val entryMap = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entryMap[entry.name] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        if (entryMap.isEmpty()) return ""

        // 1. Locate root opf file from container.xml
        val containerBytes = entryMap["META-INF/container.xml"]
        var opfPath = ""
        if (containerBytes != null) {
            val xml = String(containerBytes, Charsets.UTF_8)
            val fullPathRegex = Regex("""full-path\s*=\s*["']([^"']+)["']""")
            val match = fullPathRegex.find(xml)
            if (match != null) {
                opfPath = match.groupValues[1]
            }
        }

        if (opfPath.isEmpty()) {
            // Fallback search for any .opf entry
            opfPath = entryMap.keys.firstOrNull { it.endsWith(".opf", ignoreCase = true) } ?: ""
        }

        val opfDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
        val opfBytes = entryMap[opfPath]

        val orderedFiles = mutableListOf<String>()

        if (opfBytes != null) {
            try {
                val manifestItems = mutableMapOf<String, String>() // id -> href
                val spineIds = mutableListOf<String>()

                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(opfBytes), "UTF-8")

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        when (parser.name.lowercase()) {
                            "item" -> {
                                val id = parser.getAttributeValue(null, "id") ?: ""
                                val href = parser.getAttributeValue(null, "href") ?: ""
                                if (id.isNotEmpty() && href.isNotEmpty()) {
                                    manifestItems[id] = href
                                }
                            }
                            "itemref" -> {
                                val idref = parser.getAttributeValue(null, "idref") ?: ""
                                if (idref.isNotEmpty()) {
                                    spineIds.add(idref)
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                // Gather ordered hrefs
                for (idref in spineIds) {
                    val href = manifestItems[idref]
                    if (href != null) {
                        val fullPath = if (href.startsWith("/")) href.substring(1) else opfDir + href
                        // Resolve relative ../
                        val normalized = normalizePath(fullPath)
                        orderedFiles.add(normalized)
                    }
                }
            } catch (_: Exception) {
                // Ignore parse errors, fallback below
            }
        }

        // If no spine was resolved, grab all html/xhtml files
        if (orderedFiles.isEmpty()) {
            orderedFiles.addAll(
                entryMap.keys.filter { it.endsWith(".html", true) || it.endsWith(".xhtml", true) || it.endsWith(".htm", true) }.sorted()
            )
        }

        val result = StringBuilder()
        val htmlTagRegex = Regex("""<[^>]+>""")

        for (filePath in orderedFiles) {
            val chapterBytes = entryMap[filePath] ?: continue
            val chapterHtml = decodeBytes(chapterBytes)
            // Strip styles and scripts first
            val noStyle = chapterHtml.replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")

            // Replace block tags with newlines
            val withBreaks = noStyle
                .replace(Regex("""<(p|div|h[1-6]|li|tr|br)[^>]*>""", RegexOption.IGNORE_CASE), "\n")

            // Strip remaining tags
            val rawText = withBreaks.replace(htmlTagRegex, " ")

            // Unescape common HTML entities
            val cleanText = unescapeHtml(rawText).lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n")

            if (cleanText.isNotBlank()) {
                result.append(cleanText).append("\n\n")
            }
        }

        return result.toString().trim()
    }

    private fun normalizePath(path: String): String {
        val parts = path.split("/")
        val stack = mutableListOf<String>()
        for (part in parts) {
            if (part == "..") {
                if (stack.isNotEmpty()) stack.removeAt(stack.size - 1)
            } else if (part != "." && part.isNotEmpty()) {
                stack.add(part)
            }
        }
        return stack.joinToString("/")
    }

    private fun unescapeHtml(text: String): String {
        return text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&mdash;", "—")
            .replace("&hellip;", "…")
    }
}
