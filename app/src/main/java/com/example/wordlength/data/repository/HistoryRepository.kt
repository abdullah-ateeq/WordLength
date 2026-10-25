package com.example.wordlength.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.wordlength.data.model.WordLengthHistory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class HistoryRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("word_length_history_prefs", Context.MODE_PRIVATE)

    private val _historyFlow = MutableStateFlow<List<WordLengthHistory>>(emptyList())
    val historyFlow: StateFlow<List<WordLengthHistory>> = _historyFlow.asStateFlow()

    init {
        _historyFlow.value = loadHistory()
    }

    private fun loadHistory(): List<WordLengthHistory> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        val list = mutableListOf<WordLengthHistory>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val maxLen = if (obj.has("maximumWordLength") && !obj.isNull("maximumWordLength")) {
                    obj.getInt("maximumWordLength")
                } else null

                list.add(
                    WordLengthHistory(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        text = obj.getString("text"),
                        analyzedAt = obj.getLong("analyzedAt"),
                        lengthUnit = obj.getString("lengthUnit"),
                        caseSensitive = obj.getBoolean("caseSensitive"),
                        includeNumbers = obj.getBoolean("includeNumbers"),
                        apostropheMode = obj.getString("apostropheMode"),
                        hyphenMode = obj.getString("hyphenMode"),
                        stopWordsEnabled = obj.getBoolean("stopWordsEnabled"),
                        minimumWordLength = obj.getInt("minimumWordLength"),
                        maximumWordLength = maxLen
                    )
                )
            }
        } catch (_: Exception) {
            // Error parsing history
        }
        return list
    }

    private fun persist(list: List<WordLengthHistory>) {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("analyzedAt", item.analyzedAt)
                put("lengthUnit", item.lengthUnit)
                put("caseSensitive", item.caseSensitive)
                put("includeNumbers", item.includeNumbers)
                put("apostropheMode", item.apostropheMode)
                put("hyphenMode", item.hyphenMode)
                put("stopWordsEnabled", item.stopWordsEnabled)
                put("minimumWordLength", item.minimumWordLength)
                if (item.maximumWordLength != null) {
                    put("maximumWordLength", item.maximumWordLength)
                } else {
                    put("maximumWordLength", JSONObject.NULL)
                }
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        _historyFlow.value = list
    }

    /**
     * Saves an analysis.
     * Prevents immediate duplicate (same text and settings).
     * Maintains maximum of 10 items (removes oldest if exceeds 10).
     * Returns true if saved, false if duplicate or empty.
     */
    fun saveAnalysis(
        text: String,
        lengthUnit: String,
        caseSensitive: Boolean,
        includeNumbers: Boolean,
        apostropheMode: String,
        hyphenMode: String,
        stopWordsEnabled: Boolean,
        minimumWordLength: Int,
        maximumWordLength: Int?
    ): Boolean {
        if (text.isBlank()) return false

        val current = _historyFlow.value.toMutableList()

        // Check if top item is duplicate
        val first = current.firstOrNull()
        if (first != null &&
            first.text == text &&
            first.lengthUnit == lengthUnit &&
            first.caseSensitive == caseSensitive &&
            first.includeNumbers == includeNumbers &&
            first.apostropheMode == apostropheMode &&
            first.hyphenMode == hyphenMode &&
            first.stopWordsEnabled == stopWordsEnabled &&
            first.minimumWordLength == minimumWordLength &&
            first.maximumWordLength == maximumWordLength
        ) {
            return false // Duplicate
        }

        val newItem = WordLengthHistory(
            id = UUID.randomUUID().toString(),
            text = text,
            analyzedAt = System.currentTimeMillis(),
            lengthUnit = lengthUnit,
            caseSensitive = caseSensitive,
            includeNumbers = includeNumbers,
            apostropheMode = apostropheMode,
            hyphenMode = hyphenMode,
            stopWordsEnabled = stopWordsEnabled,
            minimumWordLength = minimumWordLength,
            maximumWordLength = maximumWordLength
        )

        // Add to the top
        current.add(0, newItem)

        // Cap at 10 items (remove oldest)
        while (current.size > 10) {
            current.removeAt(current.lastIndex)
        }

        persist(current)
        return true
    }

    fun deleteItem(id: String) {
        val updated = _historyFlow.value.filterNot { it.id == id }
        persist(updated)
    }

    fun clearAll() {
        persist(emptyList())
    }

    companion object {
        private const val KEY_HISTORY = "history_entries"
    }
}
