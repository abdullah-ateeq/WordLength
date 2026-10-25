package com.example.wordlength.analysis

import com.example.wordlength.data.model.ApostropheMode
import com.example.wordlength.data.model.CaseSensitivity
import com.example.wordlength.data.model.HyphenMode
import com.example.wordlength.data.model.LengthUnit
import com.example.wordlength.data.model.NumberMode
import com.example.wordlength.data.model.StopWordsMode
import java.util.regex.Pattern

data class TokenizerOptions(
    val lengthUnit: LengthUnit = LengthUnit.GRAPHEME_CHARACTERS,
    val caseSensitivity: CaseSensitivity = CaseSensitivity.IGNORE_CASE,
    val apostropheMode: ApostropheMode = ApostropheMode.PRESERVE,
    val hyphenMode: HyphenMode = HyphenMode.KEEP,
    val numberMode: NumberMode = NumberMode.INCLUDE,
    val stopWordsMode: StopWordsMode = StopWordsMode.INCLUDE_ALL,
    val customExclusions: Set<String> = emptySet(),
    val minimumLength: Int = 1,
    val maximumLength: Int? = null,
    val ignoreUrlsAndEmails: Boolean = true,
    val stripPrefixSymbols: Boolean = true
)

object UnicodeTokenizer {

    private val URL_PATTERN = Pattern.compile("https?://\\S+|ftp://\\S+|www\\.\\S+", Pattern.CASE_INSENSITIVE)
    private val EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}", Pattern.CASE_INSENSITIVE)
    private val PREFIX_PATTERN = Pattern.compile("(?<=^|[\\s\\p{Punct}])[#@](?=[\\p{L}\\p{N}])")

    // Hyphen characters (ASCII hyphen-minus, non-breaking hyphen, figure dash, en dash, em dash, horizontal bar, minus)
    private const val HYPHEN_CHARS = "-\u2010\u2011\u2012\u2013\u2014\u2015\u2212"
    // Apostrophe characters (ASCII single quote, right single quote, modifier letter apostrophe)
    private const val APOSTROPHE_CHARS = "'\u2019\u02BB\u02BC"

    // Regex for intra-word hyphens and apostrophes
    private val INTRA_HYPHEN_PATTERN = Pattern.compile("(?<=[\\p{L}\\p{N}])[$HYPHEN_CHARS]+(?=[\\p{L}\\p{N}])")
    private val INTRA_APOSTROPHE_PATTERN = Pattern.compile("(?<=[\\p{L}\\p{N}])[$APOSTROPHE_CHARS]+(?=[\\p{L}\\p{N}])")

    // Precompiled token patterns
    private val PATTERN_BOTH_INTRA = Pattern.compile(
        "[\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\x{1F000}-\\x{1FAFF}]+(?:[${Pattern.quote(APOSTROPHE_CHARS + HYPHEN_CHARS)}][\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\x{1F000}-\\x{1FAFF}]+)*"
    )
    private val PATTERN_APOSTROPHE_ONLY = Pattern.compile(
        "[\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\x{1F000}-\\x{1FAFF}]+(?:[${Pattern.quote(APOSTROPHE_CHARS)}][\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\x{1F000}-\\x{1FAFF}]+)*"
    )
    private val PATTERN_HYPHEN_ONLY = Pattern.compile(
        "[\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\x{1F000}-\\x{1FAFF}]+(?:[${Pattern.quote(HYPHEN_CHARS)}][\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\x{1F000}-\\x{1FAFF}]+)*"
    )
    private val PATTERN_PURE_WORDS = Pattern.compile(
        "[\\p{L}\\p{M}\\p{N}\\p{So}\\p{Sk}\\x{1F000}-\\x{1FAFF}]+"
    )

    /**
     * Extracts tokens according to the pipeline and options.
     */
    fun tokenize(text: String, options: TokenizerOptions): List<String> {
        if (text.isBlank()) return emptyList()

        var processed = text

        // 1. URL and Email handling
        if (options.ignoreUrlsAndEmails) {
            processed = URL_PATTERN.matcher(processed).replaceAll(" ")
            processed = EMAIL_PATTERN.matcher(processed).replaceAll(" ")
        }

        // 2. Hashtags and mentions
        if (options.stripPrefixSymbols) {
            processed = PREFIX_PATTERN.matcher(processed).replaceAll("")
        }

        // 3. Hyphens handling
        when (options.hyphenMode) {
            HyphenMode.SPLIT -> {
                processed = processed.replace(Regex("[$HYPHEN_CHARS]"), " ")
            }
            HyphenMode.REMOVE -> {
                processed = INTRA_HYPHEN_PATTERN.matcher(processed).replaceAll("")
            }
            HyphenMode.KEEP -> {
                // Keep intra-word hyphens
            }
        }

        // 4. Apostrophes handling
        when (options.apostropheMode) {
            ApostropheMode.SPLIT -> {
                processed = processed.replace(Regex("[$APOSTROPHE_CHARS]"), " ")
            }
            ApostropheMode.REMOVE -> {
                processed = INTRA_APOSTROPHE_PATTERN.matcher(processed).replaceAll("")
            }
            ApostropheMode.PRESERVE -> {
                // Preserve intra-word apostrophes
            }
        }

        // 5. Extract words by matching word tokens using precompiled pattern
        val pattern = when {
            options.apostropheMode == ApostropheMode.PRESERVE && options.hyphenMode == HyphenMode.KEEP -> PATTERN_BOTH_INTRA
            options.apostropheMode == ApostropheMode.PRESERVE -> PATTERN_APOSTROPHE_ONLY
            options.hyphenMode == HyphenMode.KEEP -> PATTERN_HYPHEN_ONLY
            else -> PATTERN_PURE_WORDS
        }

        val matcher = pattern.matcher(processed)
        val rawTokens = mutableListOf<String>()

        while (matcher.find()) {
            val candidate = trimOuterPunctuation(matcher.group())
            if (candidate.isNotEmpty()) {
                rawTokens.add(candidate)
            }
        }

        // 6. Number Handling
        val numberFiltered = if (options.numberMode == NumberMode.EXCLUDE) {
            rawTokens.filterNot { isNumericOnly(it) }
        } else {
            rawTokens
        }

        // 7. Case Normalization & Custom / Stop-Word exclusions
        val customExclusionsNormalized = if (options.caseSensitivity == CaseSensitivity.IGNORE_CASE) {
            options.customExclusions.map { it.lowercase() }.toSet()
        } else {
            options.customExclusions
        }

        val stopWordFiltered = numberFiltered.filter { token ->
            val checkToken = if (options.caseSensitivity == CaseSensitivity.IGNORE_CASE) token.lowercase() else token
            if (options.stopWordsMode == StopWordsMode.EXCLUDE_ENGLISH && StopWords.isStopWord(token)) {
                return@filter false
            }
            if (customExclusionsNormalized.contains(checkToken)) {
                return@filter false
            }
            true
        }

        // 8. Length filtering (min & max length)
        val lengthFiltered = if (options.minimumLength <= 1 && options.maximumLength == null) {
            stopWordFiltered
        } else {
            stopWordFiltered.filter { token ->
                val len = WordLengthCalculator.measureLength(token, options.lengthUnit)
                if (len < options.minimumLength) return@filter false
                if (options.maximumLength != null && len > options.maximumLength) return@filter false
                true
            }
        }

        return lengthFiltered
    }

    private fun trimOuterPunctuation(token: String): String {
        var start = 0
        var end = token.length
        while (start < end && isTrimChar(token[start])) {
            start++
        }
        while (end > start && isTrimChar(token[end - 1])) {
            end--
        }
        return if (start < end) token.substring(start, end) else ""
    }

    private fun isTrimChar(c: Char): Boolean {
        if (Character.isLetterOrDigit(c)) return false
        if (Character.isSurrogate(c)) return false
        val type = Character.getType(c)
        if (type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            type == Character.OTHER_SYMBOL.toInt() ||
            type == Character.MODIFIER_SYMBOL.toInt()
        ) {
            return false
        }
        return true
    }

    /**
     * Checks if a token consists purely of digits / numeric characters.
     * Alphanumerics like AI2, Version2, abc123 will return false.
     */
    fun isNumericOnly(token: String): Boolean {
        if (token.isEmpty()) return false
        var hasDigit = false
        var hasNonDigit = false

        for (cp in token.codePoints()) {
            if (Character.isDigit(cp)) {
                hasDigit = true
            } else if (cp == '.'.code || cp == ','.code || cp == '-'.code) {
                // decimal or separator
            } else {
                hasNonDigit = true
            }
        }
        return hasDigit && !hasNonDigit
    }

    /**
     * Fast counter for live statistics (characters, words, lines).
     * The word count uses the same tokenization rules.
     */
    fun computeLiveTextStats(text: String, options: TokenizerOptions): Triple<Int, Int, Int> {
        if (text.isEmpty()) return Triple(0, 0, 0)

        val characters = text.length
        val lines = if (text.isEmpty()) 0 else text.count { it == '\n' } + 1
        val words = tokenize(text, options).size

        return Triple(characters, words, lines)
    }
}
