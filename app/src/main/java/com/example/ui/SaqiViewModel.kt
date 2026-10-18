package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.engine.ToolExecutionEngine
import com.example.live.GeminiLiveSessionManager
import com.example.model.AssistantState
import com.example.model.ConnectionState
import com.example.model.LiveSessionUiState
import com.example.model.ToolExecutionRecord
import com.example.service.SaqiBackgroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SaqiViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("saqi_prefs", Context.MODE_PRIVATE)

    val toolExecutionEngine = ToolExecutionEngine(application.applicationContext)

    val liveSessionManager = GeminiLiveSessionManager(
        context = application.applicationContext,
        scope = viewModelScope,
        toolExecutionEngine = toolExecutionEngine
    )

    private val _uiState = MutableStateFlow(LiveSessionUiState())
    val uiState: StateFlow<LiveSessionUiState> = _uiState.asStateFlow()

    private val _apiKey = MutableStateFlow(loadApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    init {
        // Sync live session manager states with UI state
        liveSessionManager.connectionState.onEach { state ->
            _uiState.update { it.copy(connectionState = state) }
        }.launchIn(viewModelScope)

        liveSessionManager.assistantState.onEach { state ->
            _uiState.update { it.copy(assistantState = state) }
        }.launchIn(viewModelScope)

        liveSessionManager.micInputLevel.onEach { level ->
            _uiState.update { it.copy(micInputLevel = level) }
        }.launchIn(viewModelScope)

        liveSessionManager.outputAudioLevel.onEach { level ->
            _uiState.update { it.copy(outputAudioLevel = level) }
        }.launchIn(viewModelScope)

        liveSessionManager.lastSaqiSpeech.onEach { speech ->
            _uiState.update { it.copy(lastSaqiSpeech = speech) }
        }.launchIn(viewModelScope)

        liveSessionManager.activeToolName.onEach { tool ->
            _uiState.update { it.copy(activeToolName = tool) }
        }.launchIn(viewModelScope)

        liveSessionManager.toolRecords.onEach { records ->
            _uiState.update { it.copy(recentToolRecords = records) }
        }.launchIn(viewModelScope)

        liveSessionManager.statusMessage.onEach { msg ->
            _uiState.update { it.copy(statusText = msg) }
        }.launchIn(viewModelScope)

        // Load saved preferences
        val savedVoice = prefs.getString("voice_name", "Aoede") ?: "Aoede"
        val savedSensitivity = prefs.getFloat("wake_word_sensitivity", 0.5f)
        val isWakeWordActive = prefs.getBoolean("wake_word_active", true)

        liveSessionManager.setVoice(savedVoice)
        _uiState.update {
            it.copy(
                currentVoice = savedVoice,
                wakeWordSensitivity = savedSensitivity,
                isWakeWordActive = isWakeWordActive,
                isBackgroundServiceRunning = SaqiBackgroundService.isRunning
            )
        }

        checkPermissions()
    }

    private fun loadApiKey(): String {
        val saved = prefs.getString("gemini_api_key", "") ?: ""
        if (saved.isNotBlank()) return saved

        return try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                buildConfigKey
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun saveApiKey(newKey: String) {
        val clean = newKey.trim()
        prefs.edit().putString("gemini_api_key", clean).apply()
        _apiKey.value = clean
    }

    fun setVoice(voice: String) {
        prefs.edit().putString("voice_name", voice).apply()
        liveSessionManager.setVoice(voice)
        _uiState.update { it.copy(currentVoice = voice) }
    }

    fun setWakeWordSensitivity(sensitivity: Float) {
        prefs.edit().putFloat("wake_word_sensitivity", sensitivity).apply()
        _uiState.update { it.copy(wakeWordSensitivity = sensitivity) }
    }

    fun toggleWakeWord(active: Boolean) {
        prefs.edit().putBoolean("wake_word_active", active).apply()
        _uiState.update { it.copy(isWakeWordActive = active) }
        if (active) {
            startBackgroundService()
        } else {
            stopBackgroundService()
        }
    }

    fun startBackgroundService() {
        SaqiBackgroundService.start(getApplication())
        _uiState.update { it.copy(isBackgroundServiceRunning = true) }
    }

    fun stopBackgroundService() {
        SaqiBackgroundService.stop(getApplication())
        _uiState.update { it.copy(isBackgroundServiceRunning = false) }
    }

    fun checkPermissions() {
        val ctx = getApplication<Application>()
        val hasAudio = ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasContacts = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val hasCall = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val hasNotif = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val allGranted = hasAudio && hasContacts && hasCall && hasNotif
        _uiState.update { it.copy(hasAllPermissions = allGranted) }

        // Start background service automatically once audio permission is granted
        if (hasAudio && !SaqiBackgroundService.isRunning && _uiState.value.isWakeWordActive) {
            startBackgroundService()
        }
    }

    fun onOrbClicked() {
        val key = _apiKey.value.ifBlank {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
        liveSessionManager.toggleSession(key, _uiState.value.currentVoice)
    }

    fun triggerFromWakeWord() {
        val key = _apiKey.value.ifBlank {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
        liveSessionManager.startLiveSession(key, _uiState.value.currentVoice)
    }

    fun testToolDirectly(toolName: String, sampleArgs: Map<String, Any?>) {
        viewModelScope.launch {
            val result = toolExecutionEngine.execute(toolName, sampleArgs)
            val record = ToolExecutionRecord(
                id = "test_${System.currentTimeMillis()}",
                toolName = toolName,
                summary = result.outputMessage,
                success = result.success
            )
            _uiState.update { state ->
                state.copy(
                    recentToolRecords = listOf(record) + state.recentToolRecords.take(9),
                    statusText = result.sassyComment
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        liveSessionManager.stopSession()
    }
}
