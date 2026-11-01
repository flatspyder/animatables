package com.singleoriginapps.animatables

// Java's extended-grapheme regex keeps emoji/combining sequences intact without an ICU dependency.
internal actual fun graphemes(value: String): List<String> =
    Regex("\\X").findAll(value).map { it.value }.toList()

internal actual fun numberCodePoint(glyph: String): Int = glyph.codePointAt(0)
internal actual fun codePointString(codePoint: Int): String = String(Character.toChars(codePoint))
internal actual fun numberDigitValue(glyph: String): Int? =
    if (glyph.codePointCount(0, glyph.length) == 1) Character.digit(glyph.codePointAt(0), 10)
        .takeIf { it >= 0 } else null
