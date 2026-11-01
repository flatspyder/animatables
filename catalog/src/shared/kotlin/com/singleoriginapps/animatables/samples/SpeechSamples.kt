package com.singleoriginapps.animatables.samples

import kotlin.random.Random

/**
 * Repeatable synthetic speech envelopes for the two catalogs; contains no audio or microphone use.
 * Unequal syllable attacks, held vowels, noisy releases and phrase gaps replace periodic waves.
 * The compact level icon and activity ring use this scalar fixture.
 */
internal object SpeechSamples {
    const val FrameMillis = 60L
    const val InitialFrame = 40

    private val amplitudes: List<Float> = buildList {
        val random = Random(0x51EE7)
        repeat(32) {
            val phraseVolume = .45f + random.nextFloat() * .55f
            repeat(random.nextInt(3, 9)) {
                val peak = phraseVolume * (.25f + random.nextFloat() * .75f)
                val attack = random.nextInt(1, 3)
                val hold = random.nextInt(0, 4)
                val release = random.nextInt(2, 6)
                repeat(attack + hold + release) { frame ->
                    val envelope = when {
                        frame < attack -> (frame + 1).toFloat() / attack
                        frame < attack + hold -> 1f
                        else -> {
                            val tail = 1f - (frame - attack - hold + 1).toFloat() / (release + 1)
                            tail * tail
                        }
                    }
                    val texture = .6f + random.nextFloat() * .4f
                    val consonant = if (frame == 0 && random.nextInt(4) == 0) .18f else 0f
                    add((peak * envelope * texture + consonant).coerceIn(0f, 1f))
                }
                repeat(random.nextInt(0, 3)) { add(random.nextFloat() * .025f) }
            }
            repeat(random.nextInt(3, 13)) { add(random.nextFloat() * .012f) }
        }
    }

    fun level(frame: Int): Float =
        amplitudes[((frame % amplitudes.size) + amplitudes.size) % amplitudes.size]

}
