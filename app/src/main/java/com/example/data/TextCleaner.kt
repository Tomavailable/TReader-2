package com.example.data

import org.json.JSONArray
import org.json.JSONObject

data class TextCleaningRule(
    val id: String,
    val name: String,
    val pattern: String,
    val replacement: String = "",
    val isRegex: Boolean = true,
    val isEnabled: Boolean = true
)

object TextCleaner {

    val DEFAULT_RULES = listOf(
        TextCleaningRule(
            id = "clean_urls",
            name = "去除网址链接 (HTTP/HTTPS/WWW)",
            pattern = """https?://\S+|www\.\S+""",
            replacement = "",
            isRegex = true,
            isEnabled = true
        ),
        TextCleaningRule(
            id = "clean_dividers",
            name = "去除无意义装饰分割线 (===, ---, ***)",
            pattern = """[=\-_*~#]{3,}""",
            replacement = "",
            isRegex = true,
            isEnabled = true
        ),
        TextCleaningRule(
            id = "clean_watermarks",
            name = "去除网络小说常见水印广告",
            pattern = """(请[收保]藏本站.*?|最新域名.*?|天才一秒记住.*?|精彩小说免费阅读.*?|本章未完，请点击下一页继续阅读.*?)""",
            replacement = "",
            isRegex = true,
            isEnabled = true
        ),
        TextCleaningRule(
            id = "clean_html",
            name = "去除网页HTML标签与实体符号",
            pattern = """<[^>]+>|&(?:nbsp|amp|quot|lt|gt|#\d+);""",
            replacement = " ",
            isRegex = true,
            isEnabled = true
        ),
        TextCleaningRule(
            id = "clean_page_numbers",
            name = "去除孤立页码标记 (- 12 -, Page 1)",
            pattern = """(?m)^\s*[-—~·(]?\s*(?:Page\s*)?\d+\s*[-—~·)]?\s*$""",
            replacement = "",
            isRegex = true,
            isEnabled = true
        ),
        TextCleaningRule(
            id = "clean_extra_blank_lines",
            name = "压缩多余连续空行与首尾空白",
            pattern = """\n{3,}""",
            replacement = "\n\n",
            isRegex = true,
            isEnabled = true
        )
    )

    fun clean(rawText: String, rules: List<TextCleaningRule> = DEFAULT_RULES): String {
        if (rawText.isBlank()) return rawText
        var result = rawText
        for (rule in rules) {
            if (!rule.isEnabled) continue
            try {
                result = if (rule.isRegex) {
                    val regex = Regex(rule.pattern, RegexOption.IGNORE_CASE)
                    regex.replace(result, rule.replacement)
                } else {
                    result.replace(rule.pattern, rule.replacement, ignoreCase = true)
                }
            } catch (e: Exception) {
                // Ignore invalid user regex
            }
        }
        return result
    }

    fun serializeRules(rules: List<TextCleaningRule>): String {
        val array = JSONArray()
        for (rule in rules) {
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("pattern", rule.pattern)
                put("replacement", rule.replacement)
                put("isRegex", rule.isRegex)
                put("isEnabled", rule.isEnabled)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun deserializeRules(json: String?): List<TextCleaningRule> {
        if (json.isNullOrBlank()) return DEFAULT_RULES
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<TextCleaningRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    TextCleaningRule(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        name = obj.optString("name", "自定义清洗规则"),
                        pattern = obj.optString("pattern", ""),
                        replacement = obj.optString("replacement", ""),
                        isRegex = obj.optBoolean("isRegex", true),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_RULES else list
        } catch (e: Exception) {
            DEFAULT_RULES
        }
    }
}
