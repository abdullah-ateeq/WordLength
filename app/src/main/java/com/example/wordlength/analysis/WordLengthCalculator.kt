package com.example.wordlength.analysis

import com.example.wordlength.data.model.LengthUnit
import java.text.BreakIterator
import java.util.Locale

object WordLengthCalculator {

    private val isIcuAvailable: Boolean by lazy {
        try {
            Class.forName("android.icu.text.BreakIterator")
            true
        } catch (_: Throwable) {
            false
        }
    }

    private val javaBreakIteratorThreadLocal = ThreadLocal.withInitial {
        BreakIterator.getCharacterInstance(Locale.ROOT)
    }

    /**
     * Calculates the length of [word] according to the selected [LengthUnit].
     */
    fun measureLength(word: String, unit: LengthUnit): Int {
        if (word.isEmpty()) return 0
        return when (unit) {
            LengthUnit.GRAPHEME_CHARACTERS -> countGraphemes(word)
            LengthUnit.UNICODE_CODE_POINTS -> countCodePoints(word)
            LengthUnit.UTF16_CODE_UNITS -> countUtf16Units(word)
        }
    }

    /**
     * Measures the user-perceived grapheme cluster count.
     * Uses fast ASCII check when possible, then Android ICU BreakIterator if available,
     * otherwise java.text.BreakIterator with ZWJ handling.
     */
    fun countGraphemes(text: String): Int {
        if (text.isEmpty()) return 0

        // Fast-path: pure ASCII text has 1:1 code unit to grapheme ratio
        var isAscii = true
        for (i in 0 until text.length) {
            if (text[i].code > 127) {
                isAscii = false
                break
            }
        }
        if (isAscii) return text.length

        if (isIcuAvailable) {
            try {
                val icuClass = Class.forName("android.icu.text.BreakIterator")
                val getCharacterInstance = icuClass.getMethod("getCharacterInstance")
                val iterator = getCharacterInstance.invoke(null)
                val setText = icuClass.getMethod("setText", String::class.java)
                val nextMethod = icuClass.getMethod("next")
                val doneField = icuClass.getField("DONE").getInt(null)

                setText.invoke(iterator, text)
                var count = 0
                while ((nextMethod.invoke(iterator) as Int) != doneField) {
                    count++
                }
                return count
            } catch (_: Throwable) {
                // Fallback
            }
        }

        return countGraphemesWithJavaBreakIterator(text)
    }

    /**
     * Fallback grapheme cluster counter using standard java.text.BreakIterator
     * with extra handling for Zero-Width-Joiner (ZWJ) sequences common in emoji.
     */
    fun countGraphemesWithJavaBreakIterator(text: String): Int {
        if (text.isEmpty()) return 0
        val iterator = javaBreakIteratorThreadLocal.get() ?: BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(text)
        var count = 0
        var start = iterator.first()
        var end = iterator.next()

        var inZwjSequence = false

        while (end != BreakIterator.DONE) {
            val segment = text.substring(start, end)
            if (segment == "\u200D") {
                // Zero-Width Joiner: joins preceding and next character into one grapheme
                inZwjSequence = true
            } else if (inZwjSequence) {
                // This segment completes the join with the previous grapheme
                inZwjSequence = false
            } else {
                count++
            }
            start = end
            end = iterator.next()
        }
        return if (count > 0) count else 1
    }

    /**
     * Counts Unicode code points in [word].
     */
    fun countCodePoints(word: String): Int {
        return word.codePointCount(0, word.length)
    }

    /**
     * Counts UTF-16 code units (standard String.length).
     */
    fun countUtf16Units(word: String): Int {
        return word.length
    }
}
