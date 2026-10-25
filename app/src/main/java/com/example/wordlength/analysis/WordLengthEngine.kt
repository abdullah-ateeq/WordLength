package com.example.wordlength.analysis

import com.example.wordlength.data.model.CaseSensitivity
import com.example.wordlength.data.model.LengthGroup
import com.example.wordlength.data.model.LengthUnit
import com.example.wordlength.data.model.WordLengthResult
import com.example.wordlength.data.model.WordLengthSummary

data class AnalysisEngineOutput(
    val summary: WordLengthSummary,
    val results: List<WordLengthResult>,
    val distribution: Map<Int, Int>,
    val lengthGroups: List<LengthGroup>,
    val shortestWords: List<String>,
    val longestWords: List<String>,
    val averageUniqueLength: Double?
)

object WordLengthEngine {

    fun analyze(tokens: List<String>, unit: LengthUnit, caseSensitivity: CaseSensitivity): AnalysisEngineOutput {
        if (tokens.isEmpty()) {
            return AnalysisEngineOutput(
                summary = WordLengthSummary(
                    totalWords = 0,
                    uniqueWords = 0,
                    totalCharacters = 0,
                    minimumLength = null,
                    maximumLength = null,
                    averageLength = null,
                    medianLength = null,
                    modeLengths = emptyList(),
                    range = null
                ),
                results = emptyList(),
                distribution = emptyMap(),
                lengthGroups = emptyList(),
                shortestWords = emptyList(),
                longestWords = emptyList(),
                averageUniqueLength = null
            )
        }

        val totalWords = tokens.size

        // Calculate lengths for all valid tokens
        val tokenLengths = tokens.map { WordLengthCalculator.measureLength(it, unit) }
        val totalCharacters = tokenLengths.sum()
        val averageLength = totalCharacters.toDouble() / totalWords

        // Median length (calculated from all valid tokens)
        val sortedLengths = tokenLengths.sorted()
        val medianLength = if (sortedLengths.size % 2 == 1) {
            sortedLengths[sortedLengths.size / 2].toDouble()
        } else {
            val mid1 = sortedLengths[(sortedLengths.size / 2) - 1]
            val mid2 = sortedLengths[sortedLengths.size / 2]
            (mid1 + mid2) / 2.0
        }

        val minimumLength = sortedLengths.first()
        val maximumLength = sortedLengths.last()
        val range = maximumLength - minimumLength

        // Distribution: length -> frequency count
        val distributionMap = sortedLengths.groupingBy { it }.eachCount().toSortedMap()

        // Mode lengths (most common word length(s))
        val maxDistCount = distributionMap.values.maxOrNull() ?: 0
        val modeLengths = distributionMap.filter { it.value == maxDistCount }.keys.toList()

        // Distinct words aggregation for WordLengthResult
        val grouped = LinkedHashMap<String, Int>()
        // Preserve original casing of first occurrence or lowercase if case-insensitive
        val displayWordMap = mutableMapOf<String, String>()

        for (t in tokens) {
            val key = if (caseSensitivity == CaseSensitivity.IGNORE_CASE) t.lowercase() else t
            grouped[key] = (grouped[key] ?: 0) + 1
            if (!displayWordMap.containsKey(key)) {
                displayWordMap[key] = t
            }
        }

        val uniqueWords = grouped.size

        val resultsList = grouped.map { (key, freq) ->
            val displayWord = displayWordMap[key] ?: key
            val length = WordLengthCalculator.measureLength(displayWord, unit)
            val percentage = (freq.toDouble() / totalWords) * 100.0
            WordLengthResult(
                word = displayWord,
                length = length,
                frequency = freq,
                percentage = percentage
            )
        }

        // Shortest & Longest unique words
        val shortestWords = resultsList
            .filter { it.length == minimumLength }
            .map { it.word }
            .distinct()

        val longestWords = resultsList
            .filter { it.length == maximumLength }
            .map { it.word }
            .distinct()

        // Average unique word length
        val averageUniqueLength = if (resultsList.isNotEmpty()) {
            resultsList.sumOf { it.length }.toDouble() / resultsList.size
        } else null

        // Length groups
        val lengthGroups = listOf(
            createLengthGroup("1–3 characters", 1..3, sortedLengths, totalWords),
            createLengthGroup("4–6 characters", 4..6, sortedLengths, totalWords),
            createLengthGroup("7–9 characters", 7..9, sortedLengths, totalWords),
            createLengthGroup("10–12 characters", 10..12, sortedLengths, totalWords),
            createLengthGroup("13+ characters", 13..Int.MAX_VALUE, sortedLengths, totalWords)
        ).filter { it.count > 0 }

        val summary = WordLengthSummary(
            totalWords = totalWords,
            uniqueWords = uniqueWords,
            totalCharacters = totalCharacters,
            minimumLength = minimumLength,
            maximumLength = maximumLength,
            averageLength = averageLength,
            medianLength = medianLength,
            modeLengths = modeLengths,
            range = range
        )

        return AnalysisEngineOutput(
            summary = summary,
            results = resultsList,
            distribution = distributionMap,
            lengthGroups = lengthGroups,
            shortestWords = shortestWords,
            longestWords = longestWords,
            averageUniqueLength = averageUniqueLength
        )
    }

    private fun createLengthGroup(
        label: String,
        range: IntRange,
        sortedLengths: List<Int>,
        totalWords: Int
    ): LengthGroup {
        val count = sortedLengths.count { it in range }
        val pct = if (totalWords > 0) (count.toDouble() / totalWords) * 100.0 else 0.0
        return LengthGroup(label = label, range = range, count = count, percentage = pct)
    }
}
