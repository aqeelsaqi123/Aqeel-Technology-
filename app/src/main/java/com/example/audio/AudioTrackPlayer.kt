package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

class AudioTrackPlayer(
    private val onPlaybackLevel: (Float) -> Unit,
    private val onPlaybackFinished: () -> Unit
) {

    val sampleRate = 24000
    private val channelConfig = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioTrack: AudioTrack? = null
    private val audioQueue = LinkedBlockingQueue<ByteArray>()
    private val isPlaying = AtomicBoolean(false)
    private var playbackJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (isPlaying.get()) return

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufferSize * 2, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying.set(true)

            playbackJob = scope.launch(Dispatchers.IO) {
                while (isActive && isPlaying.get()) {
                    val chunk = audioQueue.poll()
                    if (chunk != null) {
                        // Calculate output level
                        val level = calculateRmsLevel(chunk)
                        onPlaybackLevel(level)

                        audioTrack?.write(chunk, 0, chunk.size)
                    } else {
                        // When queue is empty, set level to 0 and notify
                        onPlaybackLevel(0f)
                        if (audioQueue.isEmpty()) {
                            onPlaybackFinished()
                        }
                        Thread.sleep(20)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun enqueueAudio(data: ByteArray) {
        if (isPlaying.get()) {
            audioQueue.offer(data)
        }
    }

    fun interrupt() {
        audioQueue.clear()
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onPlaybackLevel(0f)
        onPlaybackFinished()
    }

    fun stop() {
        isPlaying.set(false)
        audioQueue.clear()
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
        onPlaybackLevel(0f)
    }

    private fun calculateRmsLevel(pcmBytes: ByteArray): Float {
        var sum = 0.0
        val sampleCount = pcmBytes.size / 2
        if (sampleCount == 0) return 0f

        for (i in 0 until sampleCount) {
            val sample = (pcmBytes[i * 2 + 1].toInt() shl 8) or (pcmBytes[i * 2].toInt() and 0xFF)
            sum += (sample * sample).toDouble()
        }
        val rms = sqrt(sum / sampleCount)
        return (rms / 9000.0).toFloat().coerceIn(0f, 1f)
    }
}
