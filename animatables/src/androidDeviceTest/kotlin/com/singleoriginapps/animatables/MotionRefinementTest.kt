package com.singleoriginapps.animatables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class MotionRefinementTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun refreshNeverMovesRetainedContentSidewaysInACenteredHost() {
        compose.mainClock.autoAdvance = false
        val loading = mutableStateOf(false)
        val error = mutableStateOf<String?>(null)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp), contentAlignment = Alignment.Center) {
                    RefreshableContent(
                        true, loading.value, error = error.value,
                        placeholder = {}, empty = {}) {
                        Text("Selected place", Modifier.testTag("retained"))
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(700)
        val original = compose.onNodeWithTag("retained").fetchSemanticsNode().boundsInRoot
        listOf(true, false, true, false).forEachIndexed { index, value ->
            compose.runOnIdle {
                loading.value = value
                error.value = if (index == 1) "Offline. Saved places are available." else null
            }
            repeat(12) {
                compose.mainClock.advanceTimeBy(32)
                val bounds = compose.onNodeWithTag("retained").fetchSemanticsNode().boundsInRoot
                assertEquals(
                    "Content stays horizontally anchored during notice handoff",
                    original.left,
                    bounds.left,
                    .5f
                )
                assertEquals(original.width, bounds.width, .5f)
            }
        }
    }

    @Test
    fun badgeResizesThroughIntermediateWidthsAndReversesToTheLatestCount() {
        compose.mainClock.autoAdvance = false
        val count = mutableStateOf(9)
        val motion = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                AnimatablesMotion(motion.value) {
                    AnimatedCountBadge(count.value, Modifier.testTag("badge"))
                }
            }
        }
        compose.mainClock.advanceTimeBy(700)
        fun width() = compose.onNodeWithTag("badge").fetchSemanticsNode().boundsInRoot.width
        val single = width()
        compose.runOnIdle { count.value = 12 }
        compose.mainClock.advanceTimeBy(64)
        val growing = width()
        compose.mainClock.advanceTimeBy(700)
        val double = width()
        assertTrue(
            "The capsule grows smoothly rather than jumping",
            growing > single && growing < double
        )
        compose.runOnIdle { count.value = 9 }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { count.value = 100 }
        compose.mainClock.advanceTimeBy(700)
        val settled = compose.onNodeWithTag("badge").captureToImage()
        compose.runOnIdle { motion.value = false }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(0, changedPixels(settled, compose.onNodeWithTag("badge").captureToImage()))
    }

    @Test
    fun lightShimmerUsesALighterSpotlightOverDarkInk() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Surface(color = androidx.compose.ui.graphics.Color.White) {
                    ShimmerText(
                        "████████████████", Modifier.testTag("shimmer"),
                        style = MaterialTheme.typography.headlineMedium.copy(color = androidx.compose.ui.graphics.Color.Black)
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val dark = compose.onNodeWithTag("shimmer").captureToImage()
        compose.mainClock.advanceTimeBy(960)
        val spotlight = compose.onNodeWithTag("shimmer").captureToImage()
        val a = dark.toPixelMap()
        val b = spotlight.toPixelMap()
        var lighter = 0
        var darker = 0
        var solidInk = 0
        var brightInk = 0
        for (y in 0 until dark.height) for (x in 0 until dark.width) {
            val difference = b[x, y].red - a[x, y].red
            if (a[x, y].red < .05f) {
                solidInk++
                if (difference > .4f) brightInk++
            }
            if (difference > .04f) lighter++
            if (difference < -.04f) darker++
        }
        assertTrue("The travelling band must lighten dark glyphs", lighter > 30)
        assertEquals("Ink outside the spotlight must not be dimmed", 0, darker)
        assertTrue(
            "The spotlight has a broad bright centre, not just a faint point",
            brightInk > solidInk * .3f
        )
    }

    @Test
    fun defaultShimmerRemainsVisibleInLightModePastTwoSweepsAndStopsImmediately() {
        compose.mainClock.autoAdvance = false
        val active = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Surface {
                    ShimmerText(
                        "Planning the next move",
                        Modifier.testTag("shimmer"),
                        active = active.value,
                        style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(32)
        val static = compose.onNodeWithTag("shimmer").captureToImage()
        compose.runOnIdle { active.value = true }
        compose.mainClock.advanceTimeBy(5100)
        assertTrue(
            "Default shimmer continues and has a visible band on light primary text",
            changedPixels(static, compose.onNodeWithTag("shimmer").captureToImage()) > 30
        )
        compose.runOnIdle { active.value = false }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(0, changedPixels(static, compose.onNodeWithTag("shimmer").captureToImage()))
    }
}

private fun changedPixels(first: ImageBitmap, second: ImageBitmap): Int {
    assertEquals(first.width, second.width)
    assertEquals(first.height, second.height)
    val a = first.toPixelMap()
    val b = second.toPixelMap()
    var changed = 0
    for (y in 0 until first.height) for (x in 0 until first.width) {
        if (kotlin.math.abs(a[x, y].red - b[x, y].red) > .04f) changed++
    }
    return changed
}

class MotionRefinementZeroScaleTest {
    @get:Rule
    val compose =
        createComposeRule(effectContext = object : androidx.compose.ui.MotionDurationScale {
            override val scaleFactor = 0f
        })

    @Test
    fun continuousShimmerHasNoOngoingFramesWithSystemAnimationsDisabled() {
        compose.setContent {
            MaterialTheme {
                Surface {
                    ShimmerText("A readable resting label", Modifier.testTag("shimmer"))
                }
            }
        }
        val first = compose.onNodeWithTag("shimmer").captureToImage()
        compose.mainClock.advanceTimeBy(5100)
        assertEquals(0, changedPixels(first, compose.onNodeWithTag("shimmer").captureToImage()))
    }
}
