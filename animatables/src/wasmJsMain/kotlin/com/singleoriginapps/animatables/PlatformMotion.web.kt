@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.singleoriginapps.animatables

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private fun reducedMotion(): Boolean =
    js("typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches")

private fun observeMotion(listener: (Boolean) -> Unit): JsAny = js(
    """{
    const media = window.matchMedia('(prefers-reduced-motion: reduce)');
    const handler = () => listener(media.matches);
    media.addEventListener('change', handler);
    handler();
    return {media, handler};
}"""
)

private fun stopObserving(token: JsAny): Unit =
    js("token.media.removeEventListener('change', token.handler)")

@Composable
internal actual fun rememberPlatformMotionEnabled(): Boolean {
    var reduced by remember { mutableStateOf(reducedMotion()) }
    DisposableEffect(Unit) {
        val observer = observeMotion { reduced = it }
        onDispose { stopObserving(observer) }
    }
    return !reduced
}
