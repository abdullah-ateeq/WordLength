package com.example.wordlength.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.example.wordlength.analysis.TokenizerOptions
import com.example.wordlength.data.model.ApostropheMode
import com.example.wordlength.data.model.AppTheme
import com.example.wordlength.data.model.CaseSensitivity
import com.example.wordlength.data.model.HyphenMode
import com.example.wordlength.data.model.LengthUnit
import com.example.wordlength.data.model.NumberMode
import com.example.wordlength.data.model.StopWordsMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val lengthUnit: LengthUnit = LengthUnit.GRAPHEME_CHARACTERS,
    val caseSensitivity: CaseSensitivity = CaseSensitivity.IGNORE_CASE,
    val numberMode: NumberMode = NumberMode.INCLUDE,
    val apostropheMode: ApostropheMode = ApostropheMode.PRESERVE,
    val hyphenMode: HyphenMode = HyphenMode.KEEP,
    val stopWordsMode: StopWordsMode = StopWordsMode.INCLUDE_ALL,
    val minimumLength: Int = 1,
    val maximumLength: Int? = null,
    val customExclusions: Set<String> = emptySet(),
    val showMedian: Boolean = true,
    val showMode: Boolean = true,
    val showDistribution: Boolean = true,
    val showChart: Boolean = true,
    val showUniqueAnalysis: Boolean = false,
    val liveAnalysis: Boolean = true,
    val haptics: Boolean = true,
    val animations: Boolean = true,
    val confirmDelete: Boolean = true,
    val restoreLastText: Boolean = true,
    val lastText: String = ""
) {
    fun toTokenizerOptions(): TokenizerOptions {
        return TokenizerOptions(
            lengthUnit = lengthUnit,
            caseSensitivity = caseSensitivity,
            apostropheMode = apostropheMode,
            hyphenMode = hyphenMode,
            numberMode = numberMode,
            stopWordsMode = stopWordsMode,
            customExclusions = customExclusions,
            minimumLength = minimumLength,
            maximumLength = maximumLength,
            ignoreUrlsAndEmails = true,
            stripPrefixSymbols = true
        )
    }
}

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("word_length_prefs", Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString(KEY_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val lengthUnitStr = prefs.getString(KEY_LENGTH_UNIT, LengthUnit.GRAPHEME_CHARACTERS.name) ?: LengthUnit.GRAPHEME_CHARACTERS.name
        val caseSensitivityStr = prefs.getString(KEY_CASE, CaseSensitivity.IGNORE_CASE.name) ?: CaseSensitivity.IGNORE_CASE.name
        val numberModeStr = prefs.getString(KEY_NUMBERS, NumberMode.INCLUDE.name) ?: NumberMode.INCLUDE.name
        val apostropheModeStr = prefs.getString(KEY_APOSTROPHE, ApostropheMode.PRESERVE.name) ?: ApostropheMode.PRESERVE.name
        val hyphenModeStr = prefs.getString(KEY_HYPHEN, HyphenMode.KEEP.name) ?: HyphenMode.KEEP.name
        val stopWordsStr = prefs.getString(KEY_STOP_WORDS, StopWordsMode.INCLUDE_ALL.name) ?: StopWordsMode.INCLUDE_ALL.name
        val minLen = prefs.getInt(KEY_MIN_LENGTH, 1)
        val hasMaxLen = prefs.getBoolean(KEY_HAS_MAX_LENGTH, false)
        val maxLen = if (hasMaxLen) prefs.getInt(KEY_MAX_LENGTH, 20) else null
        val exclusions = prefs.getStringSet(KEY_CUSTOM_EXCLUSIONS, emptySet()) ?: emptySet()

        return AppSettings(
            theme = try { AppTheme.valueOf(themeStr) } catch (_: Exception) { AppTheme.SYSTEM },
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            lengthUnit = try { LengthUnit.valueOf(lengthUnitStr) } catch (_: Exception) { LengthUnit.GRAPHEME_CHARACTERS },
            caseSensitivity = try { CaseSensitivity.valueOf(caseSensitivityStr) } catch (_: Exception) { CaseSensitivity.IGNORE_CASE },
            numberMode = try { NumberMode.valueOf(numberModeStr) } catch (_: Exception) { NumberMode.INCLUDE },
            apostropheMode = try { ApostropheMode.valueOf(apostropheModeStr) } catch (_: Exception) { ApostropheMode.PRESERVE },
            hyphenMode = try { HyphenMode.valueOf(hyphenModeStr) } catch (_: Exception) { HyphenMode.KEEP },
            stopWordsMode = try { StopWordsMode.valueOf(stopWordsStr) } catch (_: Exception) { StopWordsMode.INCLUDE_ALL },
            minimumLength = minLen,
            maximumLength = maxLen,
            customExclusions = exclusions,
            showMedian = prefs.getBoolean(KEY_SHOW_MEDIAN, true),
            showMode = prefs.getBoolean(KEY_SHOW_MODE, true),
            showDistribution = prefs.getBoolean(KEY_SHOW_DISTRIBUTION, true),
            showChart = prefs.getBoolean(KEY_SHOW_CHART, true),
            showUniqueAnalysis = prefs.getBoolean(KEY_SHOW_UNIQUE_ANALYSIS, false),
            liveAnalysis = prefs.getBoolean(KEY_LIVE_ANALYSIS, true),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            animations = prefs.getBoolean(KEY_ANIMATIONS, true),
            confirmDelete = prefs.getBoolean(KEY_CONFIRM_DELETE, true),
            restoreLastText = prefs.getBoolean(KEY_RESTORE_LAST_TEXT, true),
            lastText = prefs.getString(KEY_LAST_TEXT, "") ?: ""
        )
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val newSettings = transform(_settingsFlow.value)
        saveSettings(newSettings)
        _settingsFlow.value = newSettings
    }

    private fun saveSettings(s: AppSettings) {
        prefs.edit().apply {
            putString(KEY_THEME, s.theme.name)
            putBoolean(KEY_DYNAMIC_COLOR, s.dynamicColor)
            putString(KEY_LENGTH_UNIT, s.lengthUnit.name)
            putString(KEY_CASE, s.caseSensitivity.name)
            putString(KEY_NUMBERS, s.numberMode.name)
            putString(KEY_APOSTROPHE, s.apostropheMode.name)
            putString(KEY_HYPHEN, s.hyphenMode.name)
            putString(KEY_STOP_WORDS, s.stopWordsMode.name)
            putInt(KEY_MIN_LENGTH, s.minimumLength)
            if (s.maximumLength != null) {
                putBoolean(KEY_HAS_MAX_LENGTH, true)
                putInt(KEY_MAX_LENGTH, s.maximumLength)
            } else {
                putBoolean(KEY_HAS_MAX_LENGTH, false)
                remove(KEY_MAX_LENGTH)
            }
            putStringSet(KEY_CUSTOM_EXCLUSIONS, s.customExclusions)
            putBoolean(KEY_SHOW_MEDIAN, s.showMedian)
            putBoolean(KEY_SHOW_MODE, s.showMode)
            putBoolean(KEY_SHOW_DISTRIBUTION, s.showDistribution)
            putBoolean(KEY_SHOW_CHART, s.showChart)
            putBoolean(KEY_SHOW_UNIQUE_ANALYSIS, s.showUniqueAnalysis)
            putBoolean(KEY_LIVE_ANALYSIS, s.liveAnalysis)
            putBoolean(KEY_HAPTICS, s.haptics)
            putBoolean(KEY_ANIMATIONS, s.animations)
            putBoolean(KEY_CONFIRM_DELETE, s.confirmDelete)
            putBoolean(KEY_RESTORE_LAST_TEXT, s.restoreLastText)
            putString(KEY_LAST_TEXT, s.lastText)
            apply()
        }
    }

    fun resetSettings() {
        val defaultSettings = AppSettings(
            lastText = _settingsFlow.value.lastText // preserve last text
        )
        saveSettings(defaultSettings)
        _settingsFlow.value = defaultSettings
    }

    companion object {
        private const val KEY_THEME = "pref_theme"
        private const val KEY_DYNAMIC_COLOR = "pref_dynamic_color"
        private const val KEY_LENGTH_UNIT = "pref_length_unit"
        private const val KEY_CASE = "pref_case"
        private const val KEY_NUMBERS = "pref_numbers"
        private const val KEY_APOSTROPHE = "pref_apostrophe"
        private const val KEY_HYPHEN = "pref_hyphen"
        private const val KEY_STOP_WORDS = "pref_stop_words"
        private const val KEY_MIN_LENGTH = "pref_min_length"
        private const val KEY_HAS_MAX_LENGTH = "pref_has_max_length"
        private const val KEY_MAX_LENGTH = "pref_max_length"
        private const val KEY_CUSTOM_EXCLUSIONS = "pref_custom_exclusions"
        private const val KEY_SHOW_MEDIAN = "pref_show_median"
        private const val KEY_SHOW_MODE = "pref_show_mode"
        private const val KEY_SHOW_DISTRIBUTION = "pref_show_distribution"
        private const val KEY_SHOW_CHART = "pref_show_chart"
        private const val KEY_SHOW_UNIQUE_ANALYSIS = "pref_show_unique_analysis"
        private const val KEY_LIVE_ANALYSIS = "pref_live_analysis"
        private const val KEY_HAPTICS = "pref_haptics"
        private const val KEY_ANIMATIONS = "pref_animations"
        private const val KEY_CONFIRM_DELETE = "pref_confirm_delete"
        private const val KEY_RESTORE_LAST_TEXT = "pref_restore_last_text"
        private const val KEY_LAST_TEXT = "pref_last_text"
    }
}
