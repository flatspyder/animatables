package com.singleoriginapps.animatables

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnicodeTest {
    @Test
    fun numericAdaptersKeepFormattedUnicodeValuesIntact() {
        listOf(
            "0123456789",
            "٠١٢٣٤٥٦٧٨٩",
            "۰۱۲۳۴۵۶۷۸۹",
            "०१२३४५६७८९",
            "０１２３４５６７８９",
            "𝟘𝟙𝟚𝟛𝟜𝟝𝟞𝟟𝟠𝟡"
        ).forEach { digits ->
            val glyphs = graphemes(digits)
            assertEquals(10, glyphs.size)
            glyphs.forEachIndexed { value, glyph ->
                assertEquals(value, numberDigitValue(glyph))
                assertEquals(glyph, codePointString(numberCodePoint(glyph)))
            }
        }
        listOf(
            "",
            "12",
            "²",
            "$",
            ",",
            "👩‍💻",
            "1\u0301"
        ).forEach { assertNull(numberDigitValue(it)) }
    }

    @Test
    fun graphemesPreserveCombiningMarksAndEmoji() {
        assertEquals(listOf("€", "1", ",", "2", "3", "4", ".", "5", "0"), graphemes("€1,234.50"))
        assertEquals(listOf("e\u0301", "👩‍💻", "🇬🇧", "𝟡"), graphemes("e\u0301👩‍💻🇬🇧𝟡"))
    }
}
