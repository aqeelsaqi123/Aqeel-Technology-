package com.example.audio

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Lightweight local acoustic pattern analyzer for detecting the wake word "SAQI" (/sɑː.kiː/).
 * Tracks acoustic transitions:
 * 1. Fricative /s/ (High Zero-Crossing Rate & high frequency differential)
 * 2. Open vowel /a/ (High RMS energy, low ZCR)
 * 3. Velar plosive /k/ (Brief dip in energy followed by transient release)
 * 4. High front vowel /i/ (Voiced harmonic energy)
 */
class WakeWordDetector(
    private val onWakeWordDetected: () -> Unit
) {

    private val isEnabled = AtomicBoolean(true)
    private var sensitivity: Float = 0.5f

    private var lastTriggerTime = 0L
    private val cooldownMs = 1500L

    // State machine for "SAQI" detection
    private enum class State {
        SEARCHING_S,
        FOUND_S,
        FOUND_AA,
        FOUND_K_STOP,
        COMPLETED
    }

    private var currentState = State.SEARCHING_S
    private var stateStartTime = 0L
    private var sDurationMs = 0L
    private var aaDurationMs = 0L

    fun setSensitivity(value: Float) {
        sensitivity = value.coerceIn(0.1f, 1.0f)
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled.set(enabled)
        if (!enabled) {
            reset()
        }
    }

    fun reset() {
        currentState = State.SEARCHING_S
        stateStartTime = 0L
        sDurationMs = 0L
        aaDurationMs = 0L
    }

    /**
     * Process 16-bit PCM mono audio samples (e.g. at 16000Hz).
     */
    fun processSamples(pcmBytes: ByteArray, sampleRate: Int = 16000) {
        if (!isEnabled.get()) return

        val now = System.currentTimeMillis()
        if (now - lastTriggerTime < cooldownMs) return

        val sampleCount = pcmBytes.size / 2
        if (sampleCount < 160) return

        // Compute frame features (Energy, Zero Crossing Rate, High Frequency Ratio)
        var sumEnergy = 0.0
        var zeroCrossings = 0
        var prevSample = 0
        var highFreqDiffSum = 0.0

        for (i in 0 until sampleCount) {
            val sample = (pcmBytes[i * 2 + 1].toInt() shl 8) or (pcmBytes[i * 2].toInt() and 0xFF)
            sumEnergy += abs(sample)

            if (i > 0) {
                if ((sample >= 0 && prevSample < 0) || (sample < 0 && prevSample >= 0)) {
                    zeroCrossings++
                }
                highFreqDiffSum += abs(sample - prevSample)
            }
            prevSample = sample
        }

        val avgEnergy = sumEnergy / sampleCount
        val zcr = zeroCrossings.toFloat() / sampleCount
        val hfRatio = (highFreqDiffSum / (sumEnergy + 1.0)).toFloat()
        val frameDurationMs = (sampleCount * 1000L) / sampleRate

        // Adaptive thresholds scaled by sensitivity
        val minEnergy = (200f * (1.2f - sensitivity)).coerceAtLeast(60f)
        val sZcrMin = (0.24f * (1.2f - sensitivity * 0.4f)).coerceIn(0.16f, 0.35f)
        val sHfMin = (0.45f * (1.2f - sensitivity * 0.4f)).coerceIn(0.30f, 0.65f)

        val isSibilantS = avgEnergy > minEnergy && zcr > sZcrMin && hfRatio > sHfMin
        val isVoicedVowel = avgEnergy > (minEnergy * 1.5f) && zcr < 0.18f

        when (currentState) {
            State.SEARCHING_S -> {
                if (isSibilantS) {
                    currentState = State.FOUND_S
                    stateStartTime = now
                    sDurationMs = frameDurationMs
                }
            }

            State.FOUND_S -> {
                if (isSibilantS) {
                    sDurationMs += frameDurationMs
                    if (sDurationMs > 450) {
                        // S went on too long (e.g. background hiss), reset
                        reset()
                    }
                } else if (isVoicedVowel && sDurationMs in 60..400) {
                    // Transition from 'S' to 'AA'
                    currentState = State.FOUND_AA
                    stateStartTime = now
                    aaDurationMs = frameDurationMs
                } else if (now - stateStartTime > 500) {
                    reset()
                }
            }

            State.FOUND_AA -> {
                if (isVoicedVowel) {
                    aaDurationMs += frameDurationMs
                    if (aaDurationMs > 500) {
                        reset()
                    }
                } else if (avgEnergy < (minEnergy * 1.2f) || zcr > 0.20f) {
                    // Possible closure for 'K'
                    if (aaDurationMs in 70..450) {
                        currentState = State.FOUND_K_STOP
                        stateStartTime = now
                    } else if (now - stateStartTime > 600) {
                        reset()
                    }
                } else if (now - stateStartTime > 650) {
                    reset()
                }
            }

            State.FOUND_K_STOP -> {
                val timeInStop = now - stateStartTime
                if (timeInStop < 160) {
                    if (isVoicedVowel && avgEnergy > minEnergy) {
                        // Found terminal /i/ vowel of "SAQI"!
                        lastTriggerTime = now
                        reset()
                        onWakeWordDetected()
                    }
                } else {
                    reset()
                }
            }

            State.COMPLETED -> {
                reset()
            }
        }
    }
}
