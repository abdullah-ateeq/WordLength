package com.example.wordlength.data.model

enum class LengthUnit(val label: String, val description: String) {
    GRAPHEME_CHARACTERS("Grapheme Characters", "Counts user-perceived characters."),
    UNICODE_CODE_POINTS("Unicode Code Points", "Counts Unicode code points."),
    UTF16_CODE_UNITS("UTF-16 Code Units", "Counts Java/Kotlin string units.")
}

enum class CaseSensitivity(val label: String) {
    IGNORE_CASE("Ignore Case"),
    RESPECT_CASE("Respect Case")
}

enum class ApostropheMode(val label: String) {
    PRESERVE("Preserve"),
    REMOVE("Remove"),
    SPLIT("Split")
}

enum class HyphenMode(val label: String) {
    KEEP("Keep as one word"),
    REMOVE("Remove hyphens"),
    SPLIT("Split into words")
}

enum class NumberMode(val label: String) {
    INCLUDE("Include"),
    EXCLUDE("Exclude")
}

enum class StopWordsMode(val label: String) {
    INCLUDE_ALL("Include All Words"),
    EXCLUDE_ENGLISH("Exclude English Stop Words")
}

enum class SearchMatchMode(val label: String) {
    ANYWHERE("Anywhere"),
    BEGINNING("Beginning"),
    EXACT("Exact Match")
}

enum class WordSortOrder(val label: String) {
    LENGTH_ASC("Length (Shortest first)"),
    LENGTH_DESC("Length (Longest first)"),
    ALPHA_ASC("Alphabetical (A–Z)"),
    ALPHA_DESC("Alphabetical (Z–A)"),
    FREQ_DESC("Frequency (Highest first)"),
    FREQ_ASC("Frequency (Lowest first)")
}

enum class AppTheme(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

data class WordLengthResult(
    val word: String,
    val length: Int,
    val frequency: Int,
    val percentage: Double
)

data class WordLengthSummary(
    val totalWords: Int,
    val uniqueWords: Int,
    val totalCharacters: Int,
    val minimumLength: Int?,
    val maximumLength: Int?,
    val averageLength: Double?,
    val medianLength: Double?,
    val modeLengths: List<Int>,
    val range: Int?
)

data class WordLengthHistory(
    val id: String,
    val text: String,
    val analyzedAt: Long,
    val lengthUnit: String,
    val caseSensitive: Boolean,
    val includeNumbers: Boolean,
    val apostropheMode: String,
    val hyphenMode: String,
    val stopWordsEnabled: Boolean,
    val minimumWordLength: Int,
    val maximumWordLength: Int?
)

data class LengthGroup(
    val label: String,
    val range: IntRange,
    val count: Int,
    val percentage: Double
)

data class WordLengthUiState(
    val text: String = "",
    val results: List<WordLengthResult> = emptyList(),
    val totalWords: Int = 0,
    val uniqueWords: Int = 0,
    val totalCharacters: Int = 0,
    val minimumLength: Int? = null,
    val maximumLength: Int? = null,
    val averageLength: Double? = null,
    val medianLength: Double? = null,
    val modeLengths: List<Int> = emptyList(),
    val range: Int? = null,
    val distribution: Map<Int, Int> = emptyMap(),
    val searchQuery: String = "",
    val errorMessage: String? = null,

    // Auxiliary UI state
    val isAnalyzing: Boolean = false,
    val liveTextCharacters: Int = 0,
    val liveTextWords: Int = 0,
    val liveTextLines: Int = 0,
    val shortestWords: List<String> = emptyList(),
    val longestWords: List<String> = emptyList(),
    val averageUniqueLength: Double? = null,
    val selectedWordDetail: WordLengthResult? = null,
    val searchMatchMode: SearchMatchMode = SearchMatchMode.ANYWHERE,
    val sortOrder: WordSortOrder = WordSortOrder.LENGTH_ASC,
    val lengthGroups: List<LengthGroup> = emptyList(),
    val filterSummaryExpanded: Boolean = false,
    val chartExpanded: Boolean = true,
    val lengthGroupsExpanded: Boolean = false,
    val lengthSummaryExpanded: Boolean = false,
    val showAllShortest: Boolean = false,
    val showAllLongest: Boolean = false,
    val saveSuccessMessage: String? = null
)
