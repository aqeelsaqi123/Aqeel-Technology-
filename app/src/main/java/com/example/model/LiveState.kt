package com.example.model

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

enum class AssistantState {
    IDLE,       // Slow breathing glow
    LISTENING,  // Active mic listening waveform
    THINKING,   // Pulsing neon orbital ring
    SPEAKING    // Dynamic audio wave from SAQI output
}

data class ToolExecutionRecord(
    val id: String,
    val toolName: String,
    val summary: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveSessionUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val assistantState: AssistantState = AssistantState.IDLE,
    val statusText: String = "Tap the Orb or say \"SAQI\"",
    val currentVoice: String = "Aoede", // Aoede (sassy/confident female voice)
    val micInputLevel: Float = 0f,      // 0.0f to 1.0f for reactive Canvas animation
    val outputAudioLevel: Float = 0f,   // 0.0f to 1.0f for reactive Canvas animation
    val lastUserSpeech: String = "",
    val lastSaqiSpeech: String = "",
    val activeToolName: String? = null,
    val wakeWordSensitivity: Float = 0.5f, // 0.0 - 1.0
    val isWakeWordActive: Boolean = true,
    val isBackgroundServiceRunning: Boolean = false,
    val recentToolRecords: List<ToolExecutionRecord> = emptyList(),
    val errorMessage: String? = null,
    val hasAllPermissions: Boolean = false
)
