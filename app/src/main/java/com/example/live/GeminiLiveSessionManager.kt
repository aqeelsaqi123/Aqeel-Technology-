package com.example.live

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.audio.AudioRecorderManager
import com.example.audio.AudioTrackPlayer
import com.example.engine.ToolExecutionEngine
import com.example.model.AssistantState
import com.example.model.ConnectionState
import com.example.model.LiveProtocol
import com.example.model.ToolExecutionRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class GeminiLiveSessionManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val toolExecutionEngine: ToolExecutionEngine
) {

    private val TAG = "SaqiLiveSession"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _micInputLevel = MutableStateFlow(0f)
    val micInputLevel: StateFlow<Float> = _micInputLevel.asStateFlow()

    private val _outputAudioLevel = MutableStateFlow(0f)
    val outputAudioLevel: StateFlow<Float> = _outputAudioLevel.asStateFlow()

    private val _lastSaqiSpeech = MutableStateFlow("")
    val lastSaqiSpeech: StateFlow<String> = _lastSaqiSpeech.asStateFlow()

    private val _activeToolName = MutableStateFlow<String?>(null)
    val activeToolName: StateFlow<String?> = _activeToolName.asStateFlow()

    private val _toolRecords = MutableStateFlow<List<ToolExecutionRecord>>(emptyList())
    val toolRecords: StateFlow<List<ToolExecutionRecord>> = _toolRecords.asStateFlow()

    private val _statusMessage = MutableStateFlow("Tap the Orb or say \"SAQI\"")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private var activeVoice: String = "Aoede"
    private var activeApiKey: String = ""

    private var webSocket: WebSocket? = null
    private val isSetupComplete = AtomicBoolean(false)
    private var isSessionActive = AtomicBoolean(false)

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    // Audio Playback
    val audioTrackPlayer = AudioTrackPlayer(
        onPlaybackLevel = { level ->
            _outputAudioLevel.value = level
            if (level > 0.05f && _assistantState.value != AssistantState.SPEAKING) {
                _assistantState.value = AssistantState.SPEAKING
            }
        },
        onPlaybackFinished = {
            if (_assistantState.value == AssistantState.SPEAKING) {
                _assistantState.value = if (_connectionState.value == ConnectionState.CONNECTED) {
                    AssistantState.LISTENING
                } else {
                    AssistantState.IDLE
                }
            }
        }
    )

    // Audio Recording
    val audioRecorderManager = AudioRecorderManager(
        context = context,
        onAudioChunk = { pcmChunk ->
            if (isSessionActive.get() && isSetupComplete.get()) {
                sendAudioChunk(pcmChunk)
            }
        },
        onAudioLevel = { level ->
            _micInputLevel.value = level
            // Client-side interruption: If SAQI is speaking and user speaks with volume, interrupt!
            if (level > 0.45f && _assistantState.value == AssistantState.SPEAKING) {
                Log.d(TAG, "User voice interruption detected; cutting off playback")
                audioTrackPlayer.interrupt()
                _assistantState.value = AssistantState.LISTENING
            }
        }
    )

    fun setVoice(voice: String) {
        activeVoice = voice
    }

    fun startLiveSession(apiKey: String, voiceName: String = activeVoice) {
        if (isSessionActive.get()) return

        activeApiKey = apiKey.trim()
        activeVoice = voiceName
        isSessionActive.set(true)
        isSetupComplete.set(false)
        _connectionState.value = ConnectionState.CONNECTING
        _statusMessage.value = "Connecting to Gemini Live..."

        // Start local audio player and recorder
        audioTrackPlayer.start(scope)
        audioRecorderManager.startRecording(scope)

        connectWebSocket()
    }

    private fun connectWebSocket() {
        if (activeApiKey.isBlank()) {
            _connectionState.value = ConnectionState.ERROR
            _statusMessage.value = "Missing Gemini API Key. Tap settings to configure."
            isSessionActive.set(false)
            return
        }

        val url = "${LiveProtocol.LIVE_URL_BASE}?key=$activeApiKey"
        val request = Request.Builder().url(url).build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Gemini Live WebSocket opened. Sending setup config...")
                _statusMessage.value = "Securing voice channel with SAQI..."
                val setupJson = LiveProtocol.buildSetupJson(
                    voiceName = activeVoice
                )
                webSocket.send(setupJson)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleServerMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                if (isSessionActive.get()) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                    _statusMessage.value = "Voice session ended"
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.localizedMessage}", t)
                if (isSessionActive.get()) {
                    _connectionState.value = ConnectionState.ERROR
                    _assistantState.value = AssistantState.IDLE
                    val errorMsg = if (t.message?.contains("400") == true || t.message?.contains("403") == true) {
                        "Invalid Gemini API Key or quota exceeded."
                    } else {
                        "Connection issue: ${t.localizedMessage ?: "Network error"}"
                    }
                    _statusMessage.value = errorMsg
                }
            }
        })
    }

    private fun handleServerMessage(text: String) {
        try {
            val root = JSONObject(text)

            // 1. Setup Complete
            if (root.has("setupComplete")) {
                Log.d(TAG, "Gemini Live setupComplete confirmed!")
                isSetupComplete.set(true)
                _connectionState.value = ConnectionState.CONNECTED
                _assistantState.value = AssistantState.LISTENING
                _statusMessage.value = "SAQI is live! Speak freely."
                return
            }

            // 2. Server Content (Audio / Text / Interrupted / TurnComplete)
            if (root.has("serverContent")) {
                val serverContent = root.getJSONObject("serverContent")

                // Interrupted
                if (serverContent.optBoolean("interrupted", false)) {
                    Log.d(TAG, "Model speech interrupted by server")
                    audioTrackPlayer.interrupt()
                    _assistantState.value = AssistantState.LISTENING
                    return
                }

                // Model Turn (Audio parts)
                if (serverContent.has("modelTurn")) {
                    val modelTurn = serverContent.getJSONObject("modelTurn")
                    val parts = modelTurn.optJSONArray("parts")
                    if (parts != null) {
                        for (i in 0 until parts.length()) {
                            val part = parts.getJSONObject(i)
                            // Audio Data
                            if (part.has("inlineData")) {
                                val inlineData = part.getJSONObject("inlineData")
                                val base64Data = inlineData.optString("data")
                                if (base64Data.isNotBlank()) {
                                    val pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                    audioTrackPlayer.enqueueAudio(pcmBytes)
                                    _assistantState.value = AssistantState.SPEAKING
                                }
                            }
                            // Text Transcript
                            if (part.has("text")) {
                                val transcript = part.getString("text")
                                _lastSaqiSpeech.value = transcript
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    Log.d(TAG, "Model turn complete")
                }
            }

            // 3. Tool Calls
            if (root.has("toolCall")) {
                val toolCall = root.getJSONObject("toolCall")
                val functionCalls = toolCall.optJSONArray("functionCalls")
                if (functionCalls != null && functionCalls.length() > 0) {
                    _assistantState.value = AssistantState.THINKING
                    for (i in 0 until functionCalls.length()) {
                        val call = functionCalls.getJSONObject(i)
                        val callId = call.getString("id")
                        val funcName = call.getString("name")
                        val argsObj = call.optJSONObject("args")
                        val argsMap = mutableMapOf<String, Any?>()
                        if (argsObj != null) {
                            val keys = argsObj.keys()
                            while (keys.hasNext()) {
                                val k = keys.next()
                                argsMap[k] = argsObj.get(k)
                            }
                        }

                        _activeToolName.value = funcName
                        _statusMessage.value = "SAQI is executing $funcName..."

                        // Execute native tool
                        scope.launch(Dispatchers.Main) {
                            val result = toolExecutionEngine.execute(funcName, argsMap)
                            _activeToolName.value = null

                            // Record history
                            val record = ToolExecutionRecord(
                                id = callId,
                                toolName = funcName,
                                summary = result.outputMessage,
                                success = result.success
                            )
                            _toolRecords.value = listOf(record) + _toolRecords.value.take(9)

                            // Send Tool Response back to Gemini Live
                            val respJson = LiveProtocol.buildToolResponseJson(
                                callId = callId,
                                functionName = funcName,
                                responseData = result.toJsonObject()
                            )
                            webSocket?.send(respJson)
                            _statusMessage.value = result.sassyComment
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server message: ${e.localizedMessage}", e)
        }
    }

    private fun sendAudioChunk(pcmData: ByteArray) {
        val jsonPayload = LiveProtocol.buildAudioInputJson(pcmData, audioRecorderManager.sampleRate)
        webSocket?.send(jsonPayload)
    }

    fun stopSession() {
        isSessionActive.set(false)
        isSetupComplete.set(false)
        audioRecorderManager.stopRecording()
        audioTrackPlayer.interrupt()
        audioTrackPlayer.stop()
        try {
            webSocket?.close(1000, "User ended session")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webSocket = null
        _connectionState.value = ConnectionState.DISCONNECTED
        _assistantState.value = AssistantState.IDLE
        _micInputLevel.value = 0f
        _outputAudioLevel.value = 0f
        _activeToolName.value = null
        _statusMessage.value = "Tap the Orb or say \"SAQI\""
    }

    fun toggleSession(apiKey: String, voiceName: String = activeVoice) {
        if (isSessionActive.get()) {
            stopSession()
        } else {
            startLiveSession(apiKey, voiceName)
        }
    }
}
