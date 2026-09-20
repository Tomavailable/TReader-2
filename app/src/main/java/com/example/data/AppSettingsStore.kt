package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.ui.ReadingDisplayMode
import com.example.ui.ThemeMode

class AppSettingsStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tts_app_settings", Context.MODE_PRIVATE)

    var speaker1VoiceId: String?
        get() = prefs.getString("speaker1_voice_id", null)
        set(value) = prefs.edit().putString("speaker1_voice_id", value).apply()

    var speaker2VoiceId: String?
        get() = prefs.getString("speaker2_voice_id", null)
        set(value) = prefs.edit().putString("speaker2_voice_id", value).apply()

    var speaker3VoiceId: String?
        get() = prefs.getString("speaker3_voice_id", null)
        set(value) = prefs.edit().putString("speaker3_voice_id", value).apply()

    var speaker1Rate: Float
        get() = prefs.getFloat("speaker1_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speaker1_rate", value).apply()

    var speaker2Rate: Float
        get() = prefs.getFloat("speaker2_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speaker2_rate", value).apply()

    var speaker3Rate: Float
        get() = prefs.getFloat("speaker3_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speaker3_rate", value).apply()

    var speaker1Pitch: Float
        get() = prefs.getFloat("speaker1_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("speaker1_pitch", value).apply()

    var speaker2Pitch: Float
        get() = prefs.getFloat("speaker2_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("speaker2_pitch", value).apply()

    var speaker3Pitch: Float
        get() = prefs.getFloat("speaker3_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("speaker3_pitch", value).apply()

    var activeSpeakerIndex: Int
        get() = prefs.getInt("active_speaker_index", 0)
        set(value) = prefs.edit().putInt("active_speaker_index", value).apply()

    var multiSpeakerEnabled: Boolean
        get() = prefs.getBoolean("multi_speaker_enabled", false)
        set(value) = prefs.edit().putBoolean("multi_speaker_enabled", value).apply()

    var targetRepeatCount: Int
        get() = prefs.getInt("target_repeat_count", 1)
        set(value) = prefs.edit().putInt("target_repeat_count", value).apply()

    var isSplitEnabled: Boolean
        get() = prefs.getBoolean("is_split_enabled", false)
        set(value) = prefs.edit().putBoolean("is_split_enabled", value).apply()

    var splitMode: SplitMode
        get() {
            val name = prefs.getString("split_mode", SplitMode.SMART.name)
            return try {
                SplitMode.valueOf(name ?: SplitMode.SMART.name)
            } catch (e: Exception) {
                SplitMode.SMART
            }
        }
        set(value) = prefs.edit().putString("split_mode", value.name).apply()

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var readingDisplayMode: ReadingDisplayMode
        get() {
            val name = prefs.getString("reading_display_mode", ReadingDisplayMode.BOOK_PAGE.name)
            return try {
                ReadingDisplayMode.valueOf(name ?: ReadingDisplayMode.BOOK_PAGE.name)
            } catch (e: Exception) {
                ReadingDisplayMode.BOOK_PAGE
            }
        }
        set(value) = prefs.edit().putString("reading_display_mode", value.name).apply()

    var selectedLanguage: String
        get() = prefs.getString("selected_language", "en-US") ?: "en-US"
        set(value) = prefs.edit().putString("selected_language", value).apply()

    var isAnimationEnabled: Boolean
        get() = prefs.getBoolean("is_animation_enabled", false)
        set(value) = prefs.edit().putBoolean("is_animation_enabled", value).apply()

    var flowFontSize: Float
        get() = prefs.getFloat("flow_font_size", 18.0f)
        set(value) = prefs.edit().putFloat("flow_font_size", value).apply()

    var flowLineHeightMultiplier: Float
        get() = prefs.getFloat("flow_line_height_multiplier", 1.6f)
        set(value) = prefs.edit().putFloat("flow_line_height_multiplier", value).apply()

    var flowParagraphSpacing: Float
        get() = prefs.getFloat("flow_paragraph_spacing", 12.0f)
        set(value) = prefs.edit().putFloat("flow_paragraph_spacing", value).apply()

    var flowIndentParagraphs: Boolean
        get() = prefs.getBoolean("flow_indent_paragraphs", true)
        set(value) = prefs.edit().putBoolean("flow_indent_paragraphs", value).apply()

    var secondaryPuncts: Set<Char>
        get() {
            val str = prefs.getString("secondary_puncts", null) ?: return TextSegmenter.DEFAULT_SECONDARY_PUNCTS
            return str.toSet()
        }
        set(value) = prefs.edit().putString("secondary_puncts", value.joinToString("")).apply()

    var secondarySplitMinLength: Int
        get() = prefs.getInt("secondary_split_min_length", 30)
        set(value) = prefs.edit().putInt("secondary_split_min_length", value).apply()

    var secondarySplitScheme: SecondarySplitScheme
        get() {
            val name = prefs.getString("secondary_split_scheme", SecondarySplitScheme.SCHEME_2.name)
            return try {
                SecondarySplitScheme.valueOf(name ?: SecondarySplitScheme.SCHEME_2.name)
            } catch (e: Exception) {
                SecondarySplitScheme.SCHEME_2
            }
        }
        set(value) = prefs.edit().putString("secondary_split_scheme", value.name).apply()

    var ttsPreloadBufferEnabled: Boolean
        get() = prefs.getBoolean("tts_preload_buffer_enabled", false)
        set(value) = prefs.edit().putBoolean("tts_preload_buffer_enabled", value).apply()

    var splitOnNewline: Boolean
        get() = prefs.getBoolean("split_on_newline", true)
        set(value) = prefs.edit().putBoolean("split_on_newline", value).apply()

    var terminatorPuncts: Set<Char>
        get() {
            val str = prefs.getString("terminator_puncts", null)
            val base = if (str != null) str.toSet() else TextSegmenter.DEFAULT_TERMINATOR_PUNCTS
            return if (splitOnNewline) base + '\n' else base - '\n'
        }
        set(value) {
            splitOnNewline = value.contains('\n')
            prefs.edit().putString("terminator_puncts", value.joinToString("")).apply()
        }

    var closingPuncts: Set<Char>
        get() {
            val str = prefs.getString("closing_puncts", null) ?: return TextSegmenter.DEFAULT_CLOSING_PUNCTS
            return str.toSet()
        }
        set(value) = prefs.edit().putString("closing_puncts", value.joinToString("")).apply()

    var simplePuncts: Set<Char>
        get() {
            val str = prefs.getString("simple_puncts", null) ?: return TextSegmenter.DEFAULT_SIMPLE_PUNCTS
            return str.toSet()
        }
        set(value) = prefs.edit().putString("simple_puncts", value.joinToString("")).apply()

    var abbreviations: Set<String>
        get() {
            val str = prefs.getString("abbreviations", null) ?: return TextSegmenter.DEFAULT_ABBREVIATIONS
            return str.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        }
        set(value) = prefs.edit().putString("abbreviations", value.joinToString(",")).apply()

    private val lastBookFile = java.io.File(context.filesDir, "last_read_book.txt")

    var lastOpenedFileName: String?
        get() = prefs.getString("last_file_name", null)
        set(value) = prefs.edit().putString("last_file_name", value).apply()

    var lastOpenedFullText: String?
        get() {
            return try {
                if (lastBookFile.exists()) {
                    lastBookFile.readText()
                } else {
                    val legacy = prefs.getString("last_full_text", null)
                    if (!legacy.isNullOrEmpty()) {
                        lastBookFile.writeText(legacy)
                        prefs.edit().remove("last_full_text").apply()
                    }
                    legacy
                }
            } catch (e: Exception) {
                null
            }
        }
        set(value) {
            try {
                if (value.isNullOrEmpty()) {
                    if (lastBookFile.exists()) lastBookFile.delete()
                } else {
                    lastBookFile.writeText(value)
                }
                if (prefs.contains("last_full_text")) {
                    prefs.edit().remove("last_full_text").apply()
                }
            } catch (e: Exception) {
                // ignore
            }
        }

    var lastOpenedIndex: Int
        get() = prefs.getInt("last_index", 0)
        set(value) = prefs.edit().putInt("last_index", value).apply()

    var breathingPauseMs: Int
        get() = prefs.getInt("breathing_pause_ms", 350)
        set(value) = prefs.edit().putInt("breathing_pause_ms", value).apply()

    var isSmartPauseEnabled: Boolean
        get() = prefs.getBoolean("is_smart_pause_enabled", true)
        set(value) = prefs.edit().putBoolean("is_smart_pause_enabled", value).apply()

    var isAutoCenterScrollEnabled: Boolean
        get() = prefs.getBoolean("is_auto_center_scroll", true)
        set(value) = prefs.edit().putBoolean("is_auto_center_scroll", value).apply()

    var textCleaningRulesJson: String?
        get() = prefs.getString("text_cleaning_rules_json", null)
        set(value) = prefs.edit().putString("text_cleaning_rules_json", value).apply()
}
