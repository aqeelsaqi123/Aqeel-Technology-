package com.example.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioRecorderManager(
    private val context: Context,
    private val onAudioChunk: (ByteArray) -> Unit,
    private val onAudioLevel: (Float) -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val isRecording = AtomicBoolean(false)

    val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = maxOf(
        AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat),
        2048
    )

    fun startRecording(scope: CoroutineScope) {
        if (isRecording.get()) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to MIC
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingJob = scope.launch(Dispatchers.IO) {
                // Buffer of ~100ms (1600 samples = 3200 bytes) or 2048 bytes
                val chunk = ByteArray(2048)
                while (isActive && isRecording.get()) {
                    val readBytes = audioRecord?.read(chunk, 0, chunk.size) ?: 0
                    if (readBytes > 0) {
                        val dataToSend = chunk.copyOf(readBytes)
                        onAudioChunk(dataToSend)

                        // Compute normalized RMS level (0.0f .. 1.0f)
                        val level = calculateRmsLevel(dataToSend)
                        onAudioLevel(level)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    fun stopRecording() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
        onAudioLevel(0f)
    }

    fun isCurrentlyRecording(): Boolean = isRecording.get()

    private fun calculateRmsLevel(pcmBytes: ByteArray): Float {
        var sum = 0.0
        val sampleCount = pcmBytes.size / 2
        if (sampleCount == 0) return 0f

        for (i in 0 until sampleCount) {
            val sample = (pcmBytes[i * 2 + 1].toInt() shl 8) or (pcmBytes[i * 2].toInt() and 0xFF)
            sum += (sample * sample).toDouble()
        }
        val rms = sqrt(sum / sampleCount)
        // Normalize 0..32767 to 0..1 with a curved response
        val normalized = (rms / 8000.0).toFloat().coerceIn(0f, 1f)
        return normalized
    }
}
