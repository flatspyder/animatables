package com.singleoriginapps.animatables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AudioWaveformResponseTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun shortAudioAttacksAreVisibleAndSilenceReturnsToACenteredBaseline() {
        compose.mainClock.autoAdvance = false
        val levels = mutableStateOf(listOf(0f))
        compose.setContent {
            MaterialTheme {
                AudioWaveform(levels.value, true, Modifier.size(240.dp, 72.dp)
                    .background(Color.White).testTag("wave"), color = Color.Black)
            }
        }
        compose.mainClock.advanceTimeBy(32)
        fun capture() = compose.onNodeWithTag("wave").captureToImage()
        val baseline = verticalInkBounds(capture())
        compose.runOnIdle { levels.value = listOf(1f) }
        compose.mainClock.advanceTimeBy(80)
        val attack = verticalInkBounds(capture())
        compose.mainClock.advanceTimeBy(700)
        val peak = verticalInkBounds(capture())
        assertTrue("A brief syllable must be clearly visible before 100ms", attack.last - attack.first > (peak.last - peak.first) / 2)
        assertTrue("Bars expand around a stable centre", kotlin.math.abs(baseline.first + baseline.last - attack.first - attack.last) <= 2)
        compose.runOnIdle { levels.value = listOf(0f) }
        compose.mainClock.advanceTimeBy(80)
        val release = verticalInkBounds(capture())
        assertTrue("Release remains continuous rather than blinking off", release.last - release.first > baseline.last - baseline.first)
        compose.mainClock.advanceTimeBy(1000)
        assertEquals("Silence has a stable resting baseline", baseline, verticalInkBounds(capture()))
    }
}

private fun verticalInkBounds(image: ImageBitmap): IntRange {
    val pixels = image.toPixelMap()
    val rows = (0 until image.height).filter { y ->
        (0 until image.width).any { x -> pixels[x, y].red < .5f }
    }
    return rows.first()..rows.last()
}
