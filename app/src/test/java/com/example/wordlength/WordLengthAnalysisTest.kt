package com.example.wordlength

import com.example.wordlength.data.model.StopWordsMode
import com.example.wordlength.analysis.TokenizerOptions
import com.example.wordlength.analysis.UnicodeTokenizer
import com.example.wordlength.analysis.WordLengthCalculator
import com.example.wordlength.analysis.WordLengthEngine
import com.example.wordlength.data.model.ApostropheMode
import com.example.wordlength.data.model.CaseSensitivity
import com.example.wordlength.data.model.HyphenMode
import com.example.wordlength.data.model.LengthUnit
import com.example.wordlength.data.model.NumberMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordLengthAnalysisTest {

    @Test
    fun testBasicWordLengthAndStatistics() {
        val input = "cat house tree"
        val options = TokenizerOptions()
        val tokens = UnicodeTokenizer.tokenize(input, options)
        assertEquals(listOf("cat", "house", "tree"), tokens)

        val output = WordLengthEngine.analyze(tokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)
        val summary = output.summary

        assertEquals(3, summary.totalWords)
        assertEquals(3, summary.uniqueWords)
        assertEquals(12, summary.totalCharacters) // 3 + 5 + 4
        assertEquals(4.00, summary.averageLength ?: 0.0, 0.001)
        assertEquals(4.0, summary.medianLength ?: 0.0, 0.001)
        assertEquals(3, summary.minimumLength)
        assertEquals(5, summary.maximumLength)
        assertEquals(2, summary.range)
    }

    @Test
    fun testEvenMedian() {
        val input = "cat house tree elephant" // lengths: 3, 5, 4, 8 -> sorted: 3, 4, 5, 8
        val options = TokenizerOptions()
        val tokens = UnicodeTokenizer.tokenize(input, options)
        val output = WordLengthEngine.analyze(tokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)

        assertEquals(4, output.summary.totalWords)
        assertEquals(4.5, output.summary.medianLength ?: 0.0, 0.001)
    }

    @Test
    fun testModeLengthsMultipleTies() {
        val input = "cat dog apple tree house" // lengths: 3, 3, 5, 4, 5 -> counts: 3: 2, 4: 1, 5: 2
        val options = TokenizerOptions()
        val tokens = UnicodeTokenizer.tokenize(input, options)
        val output = WordLengthEngine.analyze(tokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)

        assertEquals(listOf(3, 5), output.summary.modeLengths)
    }

    @Test
    fun testPunctuationHandling() {
        val input = "Hello, world!"
        val options = TokenizerOptions()
        val tokens = UnicodeTokenizer.tokenize(input, options)

        assertEquals(listOf("Hello", "world"), tokens)
        assertEquals(5, WordLengthCalculator.measureLength("Hello", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(5, WordLengthCalculator.measureLength("world", LengthUnit.GRAPHEME_CHARACTERS))
    }

    @Test
    fun testApostropheModes() {
        val input = "don't can't"

        // Preserve mode
        val preserveTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(apostropheMode = ApostropheMode.PRESERVE))
        assertEquals(listOf("don't", "can't"), preserveTokens)
        assertEquals(5, WordLengthCalculator.measureLength("don't", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(5, WordLengthCalculator.measureLength("can't", LengthUnit.GRAPHEME_CHARACTERS))

        // Remove mode
        val removeTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(apostropheMode = ApostropheMode.REMOVE))
        assertEquals(listOf("dont", "cant"), removeTokens)
        assertEquals(4, WordLengthCalculator.measureLength("dont", LengthUnit.GRAPHEME_CHARACTERS))

        // Split mode
        val splitTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(apostropheMode = ApostropheMode.SPLIT))
        assertEquals(listOf("don", "t", "can", "t"), splitTokens)
        assertEquals(3, WordLengthCalculator.measureLength("don", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(1, WordLengthCalculator.measureLength("t", LengthUnit.GRAPHEME_CHARACTERS))
    }

    @Test
    fun testHyphenModes() {
        val input = "well-known"

        // Keep mode
        val keepTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(hyphenMode = HyphenMode.KEEP))
        assertEquals(listOf("well-known"), keepTokens)
        assertEquals(10, WordLengthCalculator.measureLength("well-known", LengthUnit.GRAPHEME_CHARACTERS))

        // Remove mode
        val removeTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(hyphenMode = HyphenMode.REMOVE))
        assertEquals(listOf("wellknown"), removeTokens)
        assertEquals(9, WordLengthCalculator.measureLength("wellknown", LengthUnit.GRAPHEME_CHARACTERS))

        // Split mode
        val splitTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(hyphenMode = HyphenMode.SPLIT))
        assertEquals(listOf("well", "known"), splitTokens)
        assertEquals(4, WordLengthCalculator.measureLength("well", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(5, WordLengthCalculator.measureLength("known", LengthUnit.GRAPHEME_CHARACTERS))
    }

    @Test
    fun testNumberInclusionAndExclusion() {
        val input = "hello 12345 world"

        // Numbers included
        val includedTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(numberMode = NumberMode.INCLUDE))
        assertEquals(listOf("hello", "12345", "world"), includedTokens)
        assertEquals(3, includedTokens.size)

        // Numbers excluded
        val excludedTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(numberMode = NumberMode.EXCLUDE))
        assertEquals(listOf("hello", "world"), excludedTokens)
        assertEquals(2, excludedTokens.size)
    }

    @Test
    fun testAlphanumericTokens() {
        val input = "AI2 Version2 abc123"
        val tokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(numberMode = NumberMode.EXCLUDE))
        // Alphanumeric tokens must NOT be excluded or split
        assertEquals(listOf("AI2", "Version2", "abc123"), tokens)
    }

    @Test
    fun testUnicodeSupport() {
        val input = "café 你好 مرحبا Привет বাংলা"
        val options = TokenizerOptions()
        val tokens = UnicodeTokenizer.tokenize(input, options)

        assertEquals(listOf("café", "你好", "مرحبا", "Привет", "বাংলা"), tokens)
        assertEquals(4, WordLengthCalculator.measureLength("café", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(2, WordLengthCalculator.measureLength("你好", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(5, WordLengthCalculator.measureLength("مرحبا", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(6, WordLengthCalculator.measureLength("Привет", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(2, WordLengthCalculator.measureLength("বাংলা", LengthUnit.GRAPHEME_CHARACTERS))
        assertEquals(5, WordLengthCalculator.measureLength("বাংলা", LengthUnit.UNICODE_CODE_POINTS))
    }

    @Test
    fun testGraphemeCombiningCharacters() {
        // 'e' with combining acute accent U+0301: 2 code units, but 1 grapheme cluster
        val combiningWord = "re\u0301sume\u0301" // résumé
        val graphemeLength = WordLengthCalculator.measureLength(combiningWord, LengthUnit.GRAPHEME_CHARACTERS)
        assertEquals(6, graphemeLength) // r - é - s - u - m - é = 6 graphemes
    }

    @Test
    fun testEmojiGraphemeLength() {
        // Man technologist: 👨 + \u200D + 💻
        val word = "hi👨\u200D💻"
        val length = WordLengthCalculator.measureLength(word, LengthUnit.GRAPHEME_CHARACTERS)
        assertEquals(3, length) // 'h', 'i', '👨‍💻'
    }

    @Test
    fun testNoStemming() {
        val input = "run running runs"
        val tokens = UnicodeTokenizer.tokenize(input, TokenizerOptions())
        assertEquals(listOf("run", "running", "runs"), tokens)

        val output = WordLengthEngine.analyze(tokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)
        assertEquals(3, output.results.size)
    }

    @Test
    fun testEmptyText() {
        val input = ""
        val tokens = UnicodeTokenizer.tokenize(input, TokenizerOptions())
        assertEquals(0, tokens.size)

        val output = WordLengthEngine.analyze(tokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)
        assertEquals(0, output.summary.totalWords)
        assertEquals(null, output.summary.averageLength)
        assertEquals(null, output.summary.medianLength)
    }

    @Test
    fun testFilteringMinAndMaxLength() {
        val input = "a in the house internationalization"
        // Without length filters
        val allTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions())
        assertEquals(5, allTokens.size)

        // Filter: min length 3, max length 5
        val filteredTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(minimumLength = 3, maximumLength = 5))
        assertEquals(listOf("the", "house"), filteredTokens)

        val output = WordLengthEngine.analyze(filteredTokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)
        assertEquals(2, output.summary.totalWords)
        assertEquals(3, output.summary.minimumLength)
        assertEquals(5, output.summary.maximumLength)
        assertEquals(4.0, output.summary.averageLength ?: 0.0, 0.001)
    }

    @Test
    fun testStopWordsFiltering() {
        val input = "The quick brown fox jumps over the lazy dog"
        val includedTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(stopWordsMode = StopWordsMode.INCLUDE_ALL))
        assertEquals(9, includedTokens.size)

        val excludedTokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(stopWordsMode = StopWordsMode.EXCLUDE_ENGLISH))
        // "the" and "over" are stop words
        assertTrue(!excludedTokens.contains("The"))
        assertTrue(!excludedTokens.contains("the"))
        assertTrue(!excludedTokens.contains("over"))
        assertTrue(excludedTokens.contains("quick"))
        assertTrue(excludedTokens.contains("brown"))
    }

    @Test
    fun testCustomExclusions() {
        val input = "the company will test the product"
        val options = TokenizerOptions(
            customExclusions = setOf("company", "test")
        )
        val tokens = UnicodeTokenizer.tokenize(input, options)
        assertTrue(!tokens.contains("company"))
        assertTrue(!tokens.contains("test"))
        assertTrue(tokens.contains("product"))
    }

    @Test
    fun testUrlAndEmailFiltering() {
        val input = "Visit https://example.com today or contact test@example.com for help."
        val tokens = UnicodeTokenizer.tokenize(input, TokenizerOptions(ignoreUrlsAndEmails = true))
        assertEquals(listOf("Visit", "today", "or", "contact", "for", "help"), tokens)
    }

    @Test
    fun testHashtagsAndMentions() {
        val input = "Hello #Android and @user"
        val tokens = UnicodeTokenizer.tokenize(input, TokenizerOptions())
        assertEquals(listOf("Hello", "Android", "and", "user"), tokens)
    }

    @Test
    fun testLargeTextPerformance() {
        val sampleSentence = "The quick brown fox jumps over the lazy dog and explores curious new horizons. "
        val repeatCount = 100_000 / sampleSentence.length + 10
        val largeText = sampleSentence.repeat(repeatCount)
        assertTrue(largeText.length >= 100_000)

        val startTime = System.currentTimeMillis()
        val tokens = UnicodeTokenizer.tokenize(largeText, TokenizerOptions())
        val output = WordLengthEngine.analyze(tokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)
        val duration = System.currentTimeMillis() - startTime

        assertTrue(tokens.isNotEmpty())
        assertEquals(tokens.size, output.summary.totalWords)
        assertNotNull(output.summary.averageLength)
        // Processing 100,000 characters should take less than 1.5 seconds on modern hardware
        assertTrue("Processing took ${duration}ms, expected < 1500ms", duration < 1500)
    }

    @Test
    fun testResultAccuracyAllValidOccurrences() {
        val input = "cat house tree cat" // valid words: cat(3), house(5), tree(4), cat(3)
        // Total valid words = 4, Total chars = 15. Average = 15 / 4 = 3.75
        // Sorted lengths: 3, 3, 4, 5. Median = (3 + 4) / 2 = 3.5
        // Unique words: 3 (cat, house, tree). If median was incorrectly computed from unique words: lengths 3, 4, 5 -> median 4.
        val tokens = UnicodeTokenizer.tokenize(input, TokenizerOptions())
        val output = WordLengthEngine.analyze(tokens, LengthUnit.GRAPHEME_CHARACTERS, CaseSensitivity.IGNORE_CASE)

        assertEquals(4, output.summary.totalWords)
        assertEquals(3, output.summary.uniqueWords)
        assertEquals(15, output.summary.totalCharacters)
        assertEquals(3.75, output.summary.averageLength ?: 0.0, 0.001)
        assertEquals(3.5, output.summary.medianLength ?: 0.0, 0.001)
    }
}
