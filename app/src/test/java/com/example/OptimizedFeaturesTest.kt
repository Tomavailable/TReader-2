package com.example

import com.example.data.ChapterDetector
import com.example.data.TextCleaner
import com.example.data.TextCleaningRule
import org.junit.Assert.*
import org.junit.Test

class OptimizedFeaturesTest {

    @Test
    fun testEnglishChapterDetectionPatterns() {
        val lines = listOf(
            "CHAPTER I",
            "Chapter 12: The Journey Begins",
            "Chapter One - Into the Woods",
            "PART II",
            "Book III: A New Dawn",
            "Prologue",
            "Epilogue",
            "Act I, Scene 2",
            "Unit 5: Grammar Basics",
            "Lesson 3. Advanced Listening",
            "1. Introduction to Machine Learning",
            "This is just a regular sentence that shouldn't match chapter regex."
        )

        val detected = lines.filter { ChapterDetector.isChapterTitle(it) }
        assertTrue(detected.contains("CHAPTER I"))
        assertTrue(detected.contains("Chapter 12: The Journey Begins"))
        assertTrue(detected.contains("Chapter One - Into the Woods"))
        assertTrue(detected.contains("PART II"))
        assertTrue(detected.contains("Book III: A New Dawn"))
        assertTrue(detected.contains("Prologue"))
        assertTrue(detected.contains("Epilogue"))
        assertTrue(detected.contains("Act I, Scene 2"))
        assertTrue(detected.contains("Unit 5: Grammar Basics"))
        assertTrue(detected.contains("Lesson 3. Advanced Listening"))
        assertTrue(detected.contains("1. Introduction to Machine Learning"))
        assertFalse(detected.contains("This is just a regular sentence that shouldn't match chapter regex."))
    }

    @Test
    fun testTextCleanerDefaultRules() {
        val dirtyText = """
            Welcome to the story! Check out https://example.com/books for more.
            ==============================
            请收藏本站最新域名以防走失，天才一秒记住！
            <p class="chapter-content">Alice was beginning to get very tired of sitting by her sister.</p>
            - 15 -
            
            
            
            And what is the use of a book, thought Alice, without pictures?
        """.trimIndent()

        val cleaned = TextCleaner.clean(dirtyText)
        assertFalse(cleaned.contains("https://example.com/books"))
        assertFalse(cleaned.contains("=============================="))
        assertFalse(cleaned.contains("请收藏本站最新域名以防走失"))
        assertFalse(cleaned.contains("<p class=\"chapter-content\">"))
        assertFalse(cleaned.contains("- 15 -"))
        assertTrue(cleaned.contains("Alice was beginning to get very tired"))
        assertTrue(cleaned.contains("And what is the use of a book"))
    }

    @Test
    fun testTextCleanerCustomRule() {
        val text = "Sponsored by BrandX. Today we read chapter one."
        val customRule = TextCleaningRule(
            id = "brandx",
            name = "Remove BrandX",
            pattern = "Sponsored by BrandX\\.",
            replacement = "",
            isRegex = true,
            isEnabled = true
        )
        val cleaned = TextCleaner.clean(text, listOf(customRule))
        assertEquals("Today we read chapter one.", cleaned.trim())
    }
}
