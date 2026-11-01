@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.singleoriginapps.animatables

import kotlinx.cinterop.useContents
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.rangeOfComposedCharacterSequenceAtIndex
import platform.Foundation.substringWithRange

internal actual fun graphemes(value: String): List<String> {
    val text = NSString.create(string = value)
    val result = mutableListOf<String>()
    var offset = 0uL
    while (offset < text.length) {
        val range = text.rangeOfComposedCharacterSequenceAtIndex(offset)
        result += text.substringWithRange(range)
        offset = range.useContents { location + length }
    }
    return result
}

internal actual fun numberCodePoint(glyph: String): Int =
    if (glyph.length >= 2 && glyph[0].isHighSurrogate() && glyph[1].isLowSurrogate())
        0x10000 + ((glyph[0].code - 0xD800) shl 10) + glyph[1].code - 0xDC00
    else glyph[0].code

internal actual fun codePointString(codePoint: Int): String =
    if (codePoint <= 0xFFFF) codePoint.toChar().toString()
    else {
        val adjusted = codePoint - 0x10000
        "${(0xD800 + (adjusted shr 10)).toChar()}${(0xDC00 + (adjusted and 0x3FF)).toChar()}"
    }

internal actual fun numberDigitValue(glyph: String): Int? {
    if (glyph.isEmpty() || codePointString(numberCodePoint(glyph)) != glyph) return null
    val code = numberCodePoint(glyph)
    val digits = NSCharacterSet.decimalDigitCharacterSet
    if (!digits.longCharacterIsMember(code.toUInt())) return null
    var base = code
    while (base > 0 && digits.longCharacterIsMember((base - 1).toUInt())) base--
    return (code - base) % 10
}
