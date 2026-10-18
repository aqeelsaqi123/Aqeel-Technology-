package com.example.service

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.SaqiApplication
import com.example.audio.WakeWordDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class SaqiBackgroundService : Service() {

    private val TAG = "SaqiBackgroundService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var listeningJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private val isListening = AtomicBoolean(false)

    private val wakeWordDetector = WakeWordDetector {
        handleWakeWordTriggered()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SaqiBackgroundService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isRunning = false
                return START_NOT_STICKY
            }
            ACTION_PAUSE_WAKE_WORD -> {
                wakeWordDetector.setEnabled(false)
            }
            ACTION_RESUME_WAKE_WORD -> {
                wakeWordDetector.setEnabled(true)
            }
            else -> {
                startForegroundNotification()
                startWakeWordListening()
                isRunning = true
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SaqiBackgroundService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, SaqiApplication.CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startWakeWordListening() {
        if (isListening.get()) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted; background wake-word listening idle")
            return
        }

        val sampleRate = 16000
        val bufferSize = maxOf(
            AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            2048
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }

            audioRecord?.startRecording()
            isListening.set(true)
            wakeWordDetector.setEnabled(true)

            listeningJob = serviceScope.launch {
                val buffer = ByteArray(1024)
                while (isActive && isListening.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        wakeWordDetector.processSamples(buffer.copyOf(read), sampleRate)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word AudioRecord: ${e.localizedMessage}", e)
        }
    }

    private fun handleWakeWordTriggered() {
        Log.d(TAG, "Hotword 'SAQI' detected in background service!")
        vibratePhone()

        // Launch MainActivity and trigger Live session immediately
        val wakeIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_WAKE_WORD_TRIGGERED
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        startActivity(wakeIntent)

        // Also notify broadcast receiver or listeners
        val broadcast = Intent(ACTION_WAKE_WORD_TRIGGERED)
        sendBroadcast(broadcast)
    }

    private fun vibratePhone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vm?.defaultVibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 60, 120), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 80, 60, 120), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopListening() {
        isListening.set(false)
        listeningJob?.cancel()
        listeningJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        serviceScope.cancel()
        isRunning = false
        Log.d(TAG, "SaqiBackgroundService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 4040
        const val ACTION_START_SERVICE = "com.example.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_PAUSE_WAKE_WORD = "com.example.action.PAUSE_WAKE_WORD"
        const val ACTION_RESUME_WAKE_WORD = "com.example.action.RESUME_WAKE_WORD"
        const val ACTION_WAKE_WORD_TRIGGERED = "com.example.action.WAKE_WORD_TRIGGERED"

        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, SaqiBackgroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SaqiBackgroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
