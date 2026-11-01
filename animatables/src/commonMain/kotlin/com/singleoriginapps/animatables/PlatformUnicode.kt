package com.singleoriginapps.animatables

internal expect fun numberDigitValue(glyph: String): Int?
internal expect fun numberCodePoint(glyph: String): Int
internal expect fun codePointString(codePoint: Int): String
internal expect fun graphemes(value: String): List<String>
