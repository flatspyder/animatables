package com.singleoriginapps.animatables

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.abs

/** Checks the requested visual properties rather than merely detecting an animation. */
class ReferenceFidelityTest {
    @get:Rule
    val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 1f
    })

    @Test
    fun successArrivesUpwardWithoutHorizontalDriftAndReplayRepeatsIt() {
        compose.mainClock.autoAdvance = false
        val replay = mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primaryContainer = Color.Red,
                    onPrimaryContainer = Color.Black
                )
            ) {
                Box(
                    Modifier.size(160.dp).background(Color.White).testTag("success"),
                    contentAlignment = Alignment.Center
                ) {
                    SuccessCheck(true, Modifier.size(64.dp), replayKey = replay.intValue)
                }
            }
        }
        compose.mainClock.advanceTimeBy(80)
        val arrivingImage = compose.onNodeWithTag("success").captureToImage()
        val arriving = redCenter(arrivingImage)
        compose.mainClock.advanceTimeBy(800)
        val restingImage = compose.onNodeWithTag("success").captureToImage()
        val resting = redCenter(restingImage)
        saveReferenceEvidence("success-upward-moving", arrivingImage)
        saveReferenceEvidence("success-upward-final", restingImage)
        assertTrue("Arrival must travel upward", arriving.second > resting.second + 3f)
        assertTrue(
            "Arrival must retain its horizontal centre",
            abs(arriving.first - resting.first) < 4f
        )
        compose.runOnIdle { replay.intValue++ }
        compose.mainClock.advanceTimeBy(80)
        val repeatedImage = compose.onNodeWithTag("success").captureToImage()
        val repeated = redCenter(repeatedImage)
        saveReferenceEvidence("success-upward-replay", repeatedImage)
        assertTrue("Replay repeats the upward entrance", repeated.second > resting.second + 3f)
    }

    @Test
    fun rapidlyRecompletingAVisibleSuccessMarkDoesNotJumpDown() {
        compose.mainClock.autoAdvance = false
        val complete = mutableStateOf(true)
        compose.setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primaryContainer = Color.Red,
                    onPrimaryContainer = Color.Black
                )
            ) {
                Box(
                    Modifier.size(160.dp).background(Color.White).testTag("reversal"),
                    contentAlignment = Alignment.Center
                ) {
                    SuccessCheck(complete.value, Modifier.size(64.dp))
                }
            }
        }
        compose.mainClock.advanceTimeBy(1000)
        val before = compose.onNodeWithTag("reversal").captureToImage()
        val restingCenter = redCenter(before)
        compose.runOnIdle { complete.value = false }
        compose.mainClock.advanceTimeBy(32)
        val clearing = compose.onNodeWithTag("reversal").captureToImage()
        compose.runOnIdle { complete.value = true }
        compose.mainClock.advanceTimeByFrame()
        val reversed = compose.onNodeWithTag("reversal").captureToImage()
        val reversedCenter = redCenter(reversed)
        assertTrue(
            "An interrupted clearing must not restart below while still visible",
            abs(reversedCenter.second - restingCenter.second) < 3f
        )
        assertTrue(
            "Rapid reversal keeps the same horizontal centre",
            abs(reversedCenter.first - restingCenter.first) < 3f
        )
        saveReferenceEvidence("success-reversal-before", before)
        saveReferenceEvidence("success-reversal-clearing", clearing)
        saveReferenceEvidence("success-reversal-resumed", reversed)
    }

    @Test
    fun rendererActuallySoftensTextEdgesAndRevealFinishesSharp() {
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Column {
                    Box(
                        Modifier.size(240.dp, 80.dp).background(Color.White).testTag("blurred"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Focus", style = TextStyle(color = Color.Black, fontSize = 36.sp),
                            modifier = Modifier.graphicsLayer {
                                renderEffect = motionBlurEffect(3.dp.toPx())
                            })
                    }
                    Box(
                        Modifier.size(240.dp, 80.dp).background(Color.White).testTag("sharp"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Focus", style = TextStyle(color = Color.Black, fontSize = 36.sp))
                    }
                    Box(
                        Modifier.size(240.dp, 80.dp).background(Color.White).testTag("reveal"),
                        contentAlignment = Alignment.Center
                    ) {
                        TextReveal(
                            listOf("Focus"),
                            style = TextStyle(color = Color.Black, fontSize = 36.sp)
                        )
                    }
                }
            }
        }
        val blurred = compose.onNodeWithTag("blurred").captureToImage()
        val sharp = compose.onNodeWithTag("sharp").captureToImage()
        saveReferenceEvidence("render-effect-blurred", blurred)
        saveReferenceEvidence("render-effect-sharp", sharp)
        assertTrue(
            "Native RenderEffect must produce visibly softer edges",
            softPixels(blurred) > softPixels(sharp) * 2
        )
        compose.mainClock.advanceTimeBy(48)
        val moving = compose.onNodeWithTag("reveal").captureToImage()
        compose.mainClock.advanceTimeBy(800)
        val complete = compose.onNodeWithTag("reveal").captureToImage()
        saveReferenceEvidence("text-reveal-moving", moving)
        saveReferenceEvidence("text-reveal-final", complete)
        assertTrue("The reveal must visibly resolve", difference(moving, complete) > 100)
        assertEquals("The reveal must end as sharp native text", 0, difference(sharp, complete))
    }

    @Test
    fun softFocusKeepsItsFringeOutsideFadingContentBounds() {
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier.size(80.dp).background(Color.White).testTag("blur-fringe"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(24.dp).graphicsLayer {
                            alpha = .5f
                            motionBlur(3.dp.toPx(), LayerOutsets(6.dp))
                        }.background(Color.Black),
                    )
                }
            }
        }
        val image = compose.onNodeWithTag("blur-fringe").captureToImage()
        val pixels = image.toPixelMap()
        // The 24dp square occupies the middle 30% of this 80dp capture. A soft edge must
        // remain visible beyond that square while alpha compositing is active.
        val left = (image.width * .35f).toInt()
        val right = (image.width * .65f).toInt()
        var fringe = 0
        for (y in 0 until image.height) for (x in 0 until image.width) {
            if ((x < left || x >= right) && pixels[x, y].red < .98f) fringe++
        }
        saveReferenceEvidence("soft-focus-unclipped-fringe", image)
        assertTrue("Fading content must retain the soft fringe beyond its layout bounds", fringe > 10)
    }

    @Test
    fun shimmerStillSweepsAfterOneSecondAndFinishesOnStaticText() {
        compose.mainClock.autoAdvance = false
        val active = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier.size(300.dp, 80.dp).background(Color.White).testTag("shimmer"),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerText(
                        "Finding your rhythm",
                        active = active.value,
                        style = TextStyle(color = Color.Black, fontSize = 24.sp)
                    )
                }
            }
        }
        val static = compose.onNodeWithTag("shimmer").captureToImage()
        compose.runOnIdle { active.value = true }
        compose.mainClock.advanceTimeBy(1000)
        val middle = compose.onNodeWithTag("shimmer").captureToImage()
        compose.mainClock.advanceTimeBy(400)
        val later = compose.onNodeWithTag("shimmer").captureToImage()
        saveReferenceEvidence("shimmer-baseline", static)
        saveReferenceEvidence("shimmer-middle", middle)
        saveReferenceEvidence("shimmer-later", later)
        assertTrue("Shimmer must still be moving after a second", difference(middle, later) > 30)
        assertTrue("The sweep must remain perceptible", difference(later, static) > 100)
        compose.mainClock.advanceTimeBy(3200)
        val resting = compose.onNodeWithTag("shimmer").captureToImage()
        saveReferenceEvidence("shimmer-final", resting)
        assertEquals("Finite shimmer must settle to ordinary text", 0, difference(static, resting))
        compose.runOnIdle { active.value = false }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(0, difference(static, compose.onNodeWithTag("shimmer").captureToImage()))
    }
}

private fun redCenter(image: ImageBitmap): Pair<Float, Float> {
    val pixels = image.toPixelMap()
    var mass = 0f;
    var xMoment = 0f;
    var yMoment = 0f
    for (y in 0 until image.height) for (x in 0 until image.width) {
        val c = pixels[x, y]
        val red = (c.red - maxOf(c.green, c.blue)).coerceAtLeast(0f)
        if (red > .08f) {
            mass += red; xMoment += x * red; yMoment += y * red
        }
    }
    assertTrue("The arriving mark must be visible", mass > 10f)
    return xMoment / mass to yMoment / mass
}

private fun softPixels(image: ImageBitmap): Int {
    val pixels = image.toPixelMap()
    var count = 0
    for (y in 0 until image.height) for (x in 0 until image.width) {
        if (pixels[x, y].red in .08f.. .94f) count++
    }
    return count
}

private fun difference(a: ImageBitmap, b: ImageBitmap): Int {
    assertEquals(a.width, b.width); assertEquals(a.height, b.height)
    val left = a.toPixelMap();
    val right = b.toPixelMap()
    var count = 0
    for (y in 0 until a.height) for (x in 0 until a.width) if (left[x, y] != right[x, y]) count++
    return count
}

private fun saveReferenceEvidence(name: String, image: ImageBitmap) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val directory = File(context.getExternalFilesDir(null), "motion-evidence").apply { mkdirs() }
    File(directory, "reference-$name.png").outputStream().use {
        assertTrue(
            "PNG evidence must be written",
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        )
    }
}
