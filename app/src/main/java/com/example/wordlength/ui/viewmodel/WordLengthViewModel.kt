package com.example.wordlength.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wordlength.analysis.AnalysisEngineOutput
import com.example.wordlength.analysis.UnicodeTokenizer
import com.example.wordlength.analysis.WordLengthEngine
import com.example.wordlength.data.model.ApostropheMode
import com.example.wordlength.data.model.CaseSensitivity
import com.example.wordlength.data.model.HyphenMode
import com.example.wordlength.data.model.LengthUnit
import com.example.wordlength.data.model.NumberMode
import com.example.wordlength.data.model.SearchMatchMode
import com.example.wordlength.data.model.StopWordsMode
import com.example.wordlength.data.model.WordLengthHistory
import com.example.wordlength.data.model.WordLengthResult
import com.example.wordlength.data.model.WordLengthUiState
import com.example.wordlength.data.model.WordSortOrder
import com.example.wordlength.data.preferences.AppPreferences
import com.example.wordlength.data.preferences.AppSettings
import com.example.wordlength.data.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class WordLengthViewModel(application: Application) : AndroidViewModel(application) {

    val appPreferences = AppPreferences(application)
    val historyRepository = HistoryRepository(application)

    private val _uiState = MutableStateFlow(WordLengthUiState())
    val uiState: StateFlow<WordLengthUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = appPreferences.settingsFlow
    val history: StateFlow<List<WordLengthHistory>> = historyRepository.historyFlow

    private var analysisJob: Job? = null
    private var liveStatsJob: Job? = null
    private var lastRawAnalysis: AnalysisEngineOutput? = null

    init {
        // Restore last text if enabled
        val currentSettings = settings.value
        if (currentSettings.restoreLastText && currentSettings.lastText.isNotBlank()) {
            _uiState.update { it.copy(text = currentSettings.lastText) }
            updateLiveStats(currentSettings.lastText)
            runAnalysis(currentSettings.lastText, isExplicit = false)
        }

        // Listen for settings changes and re-run analysis if text is present
        viewModelScope.launch {
            settings.collectLatest { s ->
                val currentText = _uiState.value.text
                if (currentText.isNotBlank()) {
                    updateLiveStats(currentText)
                    if (s.liveAnalysis) {
                        runAnalysis(currentText, isExplicit = false)
                    }
                }
            }
        }
    }

    fun onTextChanged(newText: String) {
        _uiState.update { it.copy(text = newText, errorMessage = null) }

        // Persist last text in background
        if (settings.value.restoreLastText) {
            appPreferences.updateSettings { it.copy(lastText = newText) }
        }

        updateLiveStats(newText)

        if (settings.value.liveAnalysis) {
            analysisJob?.cancel()
            analysisJob = viewModelScope.launch {
                delay(300) // 300 ms debounce
                runAnalysis(newText, isExplicit = false)
            }
        }
    }

    private fun updateLiveStats(text: String) {
        liveStatsJob?.cancel()
        liveStatsJob = viewModelScope.launch(Dispatchers.Default) {
            val options = settings.value.toTokenizerOptions()
            val (chars, words, lines) = UnicodeTokenizer.computeLiveTextStats(text, options)
            _uiState.update {
                it.copy(
                    liveTextCharacters = chars,
                    liveTextWords = words,
                    liveTextLines = lines
                )
            }
        }
    }

    fun analyzeExplicitly() {
        val text = _uiState.value.text
        if (text.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Enter some text to analyze.",
                    results = emptyList(),
                    totalWords = 0,
                    uniqueWords = 0,
                    totalCharacters = 0,
                    minimumLength = null,
                    maximumLength = null,
                    averageLength = null,
                    medianLength = null,
                    modeLengths = emptyList(),
                    range = null,
                    distribution = emptyMap(),
                    shortestWords = emptyList(),
                    longestWords = emptyList(),
                    averageUniqueLength = null
                )
            }
            return
        }
        runAnalysis(text, isExplicit = true)
    }

    private fun runAnalysis(text: String, isExplicit: Boolean) {
        if (text.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = if (isExplicit) "Enter some text to analyze." else null,
                    results = emptyList(),
                    totalWords = 0,
                    uniqueWords = 0,
                    totalCharacters = 0,
                    minimumLength = null,
                    maximumLength = null,
                    averageLength = null,
                    medianLength = null,
                    modeLengths = emptyList(),
                    range = null,
                    distribution = emptyMap(),
                    shortestWords = emptyList(),
                    longestWords = emptyList(),
                    averageUniqueLength = null
                )
            }
            return
        }

        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, errorMessage = null) }

            val currentSettings = settings.value
            val options = currentSettings.toTokenizerOptions()

            val output = withContext(Dispatchers.Default) {
                val tokens = UnicodeTokenizer.tokenize(text, options)
                WordLengthEngine.analyze(
                    tokens = tokens,
                    unit = currentSettings.lengthUnit,
                    caseSensitivity = currentSettings.caseSensitivity
                )
            }

            lastRawAnalysis = output

            val filteredResults = filterAndSortResults(
                output.results,
                _uiState.value.searchQuery,
                _uiState.value.searchMatchMode,
                _uiState.value.sortOrder,
                currentSettings.caseSensitivity
            )

            _uiState.update {
                it.copy(
                    isAnalyzing = false,
                    results = filteredResults,
                    totalWords = output.summary.totalWords,
                    uniqueWords = output.summary.uniqueWords,
                    totalCharacters = output.summary.totalCharacters,
                    minimumLength = output.summary.minimumLength,
                    maximumLength = output.summary.maximumLength,
                    averageLength = output.summary.averageLength,
                    medianLength = output.summary.medianLength,
                    modeLengths = output.summary.modeLengths,
                    range = output.summary.range,
                    distribution = output.distribution,
                    lengthGroups = output.lengthGroups,
                    shortestWords = output.shortestWords,
                    longestWords = output.longestWords,
                    averageUniqueLength = output.averageUniqueLength,
                    errorMessage = if (output.summary.totalWords == 0 && text.isNotBlank()) {
                        "No words matched the current filter criteria."
                    } else null
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        reapplyFilterAndSort()
    }

    fun setSearchMatchMode(mode: SearchMatchMode) {
        _uiState.update { it.copy(searchMatchMode = mode) }
        reapplyFilterAndSort()
    }

    fun setSortOrder(order: WordSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        reapplyFilterAndSort()
    }

    private fun reapplyFilterAndSort() {
        val raw = lastRawAnalysis?.results ?: return
        val filtered = filterAndSortResults(
            raw,
            _uiState.value.searchQuery,
            _uiState.value.searchMatchMode,
            _uiState.value.sortOrder,
            settings.value.caseSensitivity
        )
        _uiState.update { it.copy(results = filtered) }
    }

    private fun filterAndSortResults(
        list: List<WordLengthResult>,
        query: String,
        matchMode: SearchMatchMode,
        sortOrder: WordSortOrder,
        caseSensitivity: CaseSensitivity
    ): List<WordLengthResult> {
        val filtered = if (query.isBlank()) {
            list
        } else {
            val q = if (caseSensitivity == CaseSensitivity.IGNORE_CASE) query.lowercase() else query
            list.filter { item ->
                val w = if (caseSensitivity == CaseSensitivity.IGNORE_CASE) item.word.lowercase() else item.word
                when (matchMode) {
                    SearchMatchMode.ANYWHERE -> w.contains(q)
                    SearchMatchMode.BEGINNING -> w.startsWith(q)
                    SearchMatchMode.EXACT -> w == q
                }
            }
        }

        return when (sortOrder) {
            WordSortOrder.LENGTH_ASC -> filtered.sortedWith(compareBy<WordLengthResult> { it.length }.thenBy { it.word.lowercase() })
            WordSortOrder.LENGTH_DESC -> filtered.sortedWith(compareByDescending<WordLengthResult> { it.length }.thenBy { it.word.lowercase() })
            WordSortOrder.ALPHA_ASC -> filtered.sortedBy { it.word.lowercase() }
            WordSortOrder.ALPHA_DESC -> filtered.sortedByDescending { it.word.lowercase() }
            WordSortOrder.FREQ_DESC -> filtered.sortedWith(compareByDescending<WordLengthResult> { it.frequency }.thenBy { it.word.lowercase() })
            WordSortOrder.FREQ_ASC -> filtered.sortedWith(compareBy<WordLengthResult> { it.frequency }.thenBy { it.word.lowercase() })
        }
    }

    fun selectWordDetail(result: WordLengthResult?) {
        _uiState.update { it.copy(selectedWordDetail = result) }
    }

    fun toggleFilterSummaryExpanded() {
        _uiState.update { it.copy(filterSummaryExpanded = !it.filterSummaryExpanded) }
    }

    fun toggleChartExpanded() {
        _uiState.update { it.copy(chartExpanded = !it.chartExpanded) }
    }

    fun toggleLengthGroupsExpanded() {
        _uiState.update { it.copy(lengthGroupsExpanded = !it.lengthGroupsExpanded) }
    }

    fun toggleLengthSummaryExpanded() {
        _uiState.update { it.copy(lengthSummaryExpanded = !it.lengthSummaryExpanded) }
    }

    fun toggleShowAllShortest() {
        _uiState.update { it.copy(showAllShortest = !it.showAllShortest) }
    }

    fun toggleShowAllLongest() {
        _uiState.update { it.copy(showAllLongest = !it.showAllLongest) }
    }

    fun clearText() {
        _uiState.update {
            WordLengthUiState(
                text = "",
                filterSummaryExpanded = it.filterSummaryExpanded,
                chartExpanded = it.chartExpanded
            )
        }
        lastRawAnalysis = null
        if (settings.value.restoreLastText) {
            appPreferences.updateSettings { it.copy(lastText = "") }
        }
    }

    fun saveCurrentAnalysis(): Boolean {
        val text = _uiState.value.text
        if (text.isBlank() || _uiState.value.totalWords == 0) return false

        val s = settings.value
        val saved = historyRepository.saveAnalysis(
            text = text,
            lengthUnit = s.lengthUnit.name,
            caseSensitive = s.caseSensitivity == CaseSensitivity.RESPECT_CASE,
            includeNumbers = s.numberMode == NumberMode.INCLUDE,
            apostropheMode = s.apostropheMode.name,
            hyphenMode = s.hyphenMode.name,
            stopWordsEnabled = s.stopWordsMode == StopWordsMode.EXCLUDE_ENGLISH,
            minimumWordLength = s.minimumLength,
            maximumWordLength = s.maximumLength
        )

        if (saved) {
            _uiState.update { it.copy(saveSuccessMessage = "Analysis saved to History") }
        } else {
            _uiState.update { it.copy(saveSuccessMessage = "Already saved in History") }
        }
        return saved
    }

    fun clearSaveSuccessMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    fun restoreHistoryItem(item: WordLengthHistory) {
        // Restore settings
        appPreferences.updateSettings { current ->
            current.copy(
                lengthUnit = try { LengthUnit.valueOf(item.lengthUnit) } catch (_: Exception) { current.lengthUnit },
                caseSensitivity = if (item.caseSensitive) CaseSensitivity.RESPECT_CASE else CaseSensitivity.IGNORE_CASE,
                numberMode = if (item.includeNumbers) NumberMode.INCLUDE else NumberMode.EXCLUDE,
                apostropheMode = try { ApostropheMode.valueOf(item.apostropheMode) } catch (_: Exception) { current.apostropheMode },
                hyphenMode = try { HyphenMode.valueOf(item.hyphenMode) } catch (_: Exception) { current.hyphenMode },
                stopWordsMode = if (item.stopWordsEnabled) StopWordsMode.EXCLUDE_ENGLISH else StopWordsMode.INCLUDE_ALL,
                minimumLength = item.minimumWordLength,
                maximumLength = item.maximumWordLength,
                lastText = item.text
            )
        }

        // Restore text and trigger analysis
        _uiState.update { it.copy(text = item.text) }
        updateLiveStats(item.text)
        runAnalysis(item.text, isExplicit = true)
    }

    fun deleteHistoryItem(id: String) {
        historyRepository.deleteItem(id)
    }

    fun clearAllHistory() {
        historyRepository.clearAll()
    }

    fun copyToClipboard(context: Context, text: String, label: String = "WordLength") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun buildSummaryText(): String {
        val s = _uiState.value
        val avgStr = s.averageLength?.let { String.format(Locale.US, "%.2f", it) } ?: "—"
        val medianStr = s.medianLength?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.1f", it)
        } ?: "—"
        val shortestStr = if (s.shortestWords.isNotEmpty()) {
            "${s.shortestWords.joinToString(", ")} — ${s.minimumLength ?: 0} character${if ((s.minimumLength ?: 0) != 1) "s" else ""}"
        } else "—"
        val longestStr = if (s.longestWords.isNotEmpty()) {
            "${s.longestWords.joinToString(", ")} — ${s.maximumLength ?: 0} character${if ((s.maximumLength ?: 0) != 1) "s" else ""}"
        } else "—"
        val mostCommonStr = if (s.modeLengths.isNotEmpty() && s.distribution.isNotEmpty()) {
            s.modeLengths.joinToString(", ") { len ->
                "$len characters — ${s.distribution[len] ?: 0} words"
            }
        } else "—"

        return buildString {
            appendLine("WordLength")
            appendLine()
            appendLine("Total words: ${s.totalWords}")
            appendLine("Unique words: ${s.uniqueWords}")
            appendLine()
            appendLine("Shortest word:")
            appendLine(shortestStr)
            appendLine()
            appendLine("Longest word:")
            appendLine(longestStr)
            appendLine()
            appendLine("Average word length:")
            appendLine("$avgStr characters")
            appendLine()
            appendLine("Median:")
            appendLine("$medianStr characters")
            appendLine()
            appendLine("Most common length:")
            appendLine(mostCommonStr)
            appendLine()
            appendLine("Range:")
            appendLine("${s.range ?: 0} characters")
        }
    }

    fun buildAllWordsText(): String {
        val results = lastRawAnalysis?.results ?: _uiState.value.results
        return buildString {
            appendLine("WordLength")
            appendLine()
            appendLine(String.format(Locale.US, "%-24s %s", "Word", "Length"))
            results.forEach { item ->
                appendLine(String.format(Locale.US, "%-24s %d", item.word, item.length))
            }
        }
    }

    fun generateWordsCsv(): String {
        val results = lastRawAnalysis?.results ?: _uiState.value.results
        return buildString {
            appendLine("Word,Length,Frequency,Percentage")
            results.forEach { r ->
                val escapedWord = escapeCsv(r.word)
                val pctStr = String.format(Locale.US, "%.2f", r.percentage)
                appendLine("$escapedWord,${r.length},${r.frequency},$pctStr")
            }
        }
    }

    fun generateDistributionCsv(): String {
        val dist = _uiState.value.distribution
        val total = _uiState.value.totalWords
        return buildString {
            appendLine("Length,Word Count,Percentage")
            dist.forEach { (len, count) ->
                val pct = if (total > 0) (count.toDouble() / total) * 100.0 else 0.0
                val pctStr = String.format(Locale.US, "%.2f", pct)
                appendLine("$len,$count,$pctStr")
            }
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun shareSummary(context: Context) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, buildSummaryText())
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share WordLength Summary")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    // Settings actions
    fun updateTheme(theme: com.example.wordlength.data.model.AppTheme) {
        appPreferences.updateSettings { it.copy(theme = theme) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        appPreferences.updateSettings { it.copy(dynamicColor = enabled) }
    }

    fun updateLengthUnit(unit: LengthUnit) {
        appPreferences.updateSettings { it.copy(lengthUnit = unit) }
    }

    fun updateCaseSensitivity(case: CaseSensitivity) {
        appPreferences.updateSettings { it.copy(caseSensitivity = case) }
    }

    fun updateNumberMode(mode: NumberMode) {
        appPreferences.updateSettings { it.copy(numberMode = mode) }
    }

    fun updateApostropheMode(mode: ApostropheMode) {
        appPreferences.updateSettings { it.copy(apostropheMode = mode) }
    }

    fun updateHyphenMode(mode: HyphenMode) {
        appPreferences.updateSettings { it.copy(hyphenMode = mode) }
    }

    fun updateStopWordsMode(mode: StopWordsMode) {
        appPreferences.updateSettings { it.copy(stopWordsMode = mode) }
    }

    fun updateMinLength(min: Int) {
        appPreferences.updateSettings { it.copy(minimumLength = min) }
    }

    fun updateMaxLength(max: Int?) {
        appPreferences.updateSettings { it.copy(maximumLength = max) }
    }

    fun addCustomExclusion(word: String) {
        val trimmed = word.trim()
        if (trimmed.isNotBlank()) {
            appPreferences.updateSettings { it.copy(customExclusions = it.customExclusions + trimmed) }
        }
    }

    fun removeCustomExclusion(word: String) {
        appPreferences.updateSettings { it.copy(customExclusions = it.customExclusions - word) }
    }

    fun updateShowMedian(show: Boolean) {
        appPreferences.updateSettings { it.copy(showMedian = show) }
    }

    fun updateShowMode(show: Boolean) {
        appPreferences.updateSettings { it.copy(showMode = show) }
    }

    fun updateShowDistribution(show: Boolean) {
        appPreferences.updateSettings { it.copy(showDistribution = show) }
    }

    fun updateShowChart(show: Boolean) {
        appPreferences.updateSettings { it.copy(showChart = show) }
    }

    fun updateShowUniqueAnalysis(show: Boolean) {
        appPreferences.updateSettings { it.copy(showUniqueAnalysis = show) }
    }

    fun updateLiveAnalysis(enabled: Boolean) {
        appPreferences.updateSettings { it.copy(liveAnalysis = enabled) }
    }

    fun updateHaptics(enabled: Boolean) {
        appPreferences.updateSettings { it.copy(haptics = enabled) }
    }

    fun updateAnimations(enabled: Boolean) {
        appPreferences.updateSettings { it.copy(animations = enabled) }
    }

    fun updateConfirmDelete(enabled: Boolean) {
        appPreferences.updateSettings { it.copy(confirmDelete = enabled) }
    }

    fun updateRestoreLastText(enabled: Boolean) {
        appPreferences.updateSettings { it.copy(restoreLastText = enabled) }
    }

    fun resetSettings() {
        appPreferences.resetSettings()
    }
}
