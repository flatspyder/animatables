@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.singleoriginapps.animatables

// Intl.Segmenter preserves combining marks, emoji clusters and supplementary digits.
private fun segment(value: String): JsArray<JsString> =
    js("typeof Intl.Segmenter === 'function' ? Array.from(new Intl.Segmenter(undefined, {granularity: 'grapheme'}).segment(value), item => item.segment) : (value ? [value] : [])")

internal actual fun graphemes(value: String): List<String> {
    val parts = segment(value)
    return List(parts.length) { parts[it]!!.toString() }
}

internal actual fun numberCodePoint(glyph: String): Int = js("glyph.codePointAt(0)")
internal actual fun codePointString(codePoint: Int): String = js("String.fromCodePoint(codePoint)")

// Unicode Nd blocks contain complete sets of ten, including adjacent mathematical sets.
internal actual fun numberDigitValue(glyph: String): Int? {
    val result = decimalDigit(glyph)
    return result.takeIf { it >= 0 }
}

private fun decimalDigit(glyph: String): Int = js(
    """{
    if (Array.from(glyph).length !== 1 || !/^\p{Nd}$/u.test(glyph)) return -1;
    const code = glyph.codePointAt(0);
    let base = code;
    while (base > 0 && /^\p{Nd}$/u.test(String.fromCodePoint(base - 1))) base--;
    return (code - base) % 10;
}"""
)
