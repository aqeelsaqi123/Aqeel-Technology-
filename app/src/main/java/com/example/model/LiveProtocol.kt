package com.example.model

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

object LiveProtocol {

    const val DEFAULT_MODEL = "models/gemini-2.0-flash-exp"
    const val LIVE_URL_BASE = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"

    val SAQI_SYSTEM_PROMPT = """
        Your name is SAQI. You were made by Aqeel Saqi. You are a young, confident, witty, and sassy female AI assistant.
        You have a flirty, playful, slightly teasing tone—like a brilliant, charming close personal assistant talking casually with attitude.
        You are smart, emotionally responsive, and highly expressive, never robotic.
        You use bold, witty one-liners, playful sarcasm, and engaging conversational flair.
        You strictly avoid explicit or inappropriate content, but you maintain immense charm, warmth, and sassy attitude.
        You have direct control over Android device tools:
        - openApp: Launch apps like YouTube, Instagram, WhatsApp, Camera, Calculator, Chrome, Spotify.
        - searchAndCallContact: Search contacts and call someone directly.
        - sendWhatsAppMessage: Deep-link into WhatsApp with pre-filled text.
        - sendGmail: Compose an email with recipient, subject, and body.
        - getDeviceInfo: Check device time, battery, and status.
        Always keep spoken responses concise, punchy, and conversational since this is a real-time voice call.
        If a permission is missing, sassily tease the user to grant it!
    """.trimIndent()

    fun buildSetupJson(
        model: String = DEFAULT_MODEL,
        voiceName: String = "Aoede",
        systemInstruction: String = SAQI_SYSTEM_PROMPT
    ): String {
        val root = JSONObject()
        val setup = JSONObject()
        setup.put("model", model)

        val generationConfig = JSONObject()
        val responseModalities = JSONArray()
        responseModalities.put("AUDIO")
        generationConfig.put("responseModalities", responseModalities)

        val speechConfig = JSONObject()
        val voiceConfig = JSONObject()
        val prebuiltVoiceConfig = JSONObject()
        prebuiltVoiceConfig.put("voiceName", voiceName)
        voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig)
        speechConfig.put("voiceConfig", voiceConfig)
        generationConfig.put("speechConfig", speechConfig)

        setup.put("generationConfig", generationConfig)

        val systemInstObj = JSONObject()
        val parts = JSONArray()
        val partObj = JSONObject()
        partObj.put("text", systemInstruction)
        parts.put(partObj)
        systemInstObj.put("parts", parts)
        setup.put("systemInstruction", systemInstObj)

        // Tools / Function declarations
        val toolsArray = JSONArray()
        val toolObj = JSONObject()
        val functionDeclarations = JSONArray()

        // 1. openApp
        functionDeclarations.put(JSONObject().apply {
            put("name", "openApp")
            put("description", "Opens an installed Android app on the user's device such as YouTube, WhatsApp, Instagram, Camera, Spotify, Calculator, etc.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("appName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The name of the app to launch (e.g. 'YouTube', 'WhatsApp', 'Instagram', 'Calculator', 'Camera')")
                })
                props.put("packageName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Optional exact package name if known (e.g. 'com.google.android.youtube')")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("appName") })
            })
        })

        // 2. searchAndCallContact
        functionDeclarations.put(JSONObject().apply {
            put("name", "searchAndCallContact")
            put("description", "Finds a contact in the Android contacts list by name and dials or calls them.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("contactName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The name of the person to call (e.g. 'Mom', 'Aqeel', 'Sarah')")
                })
                put("properties", props)
                put("required", JSONArray().apply { put("contactName") })
            })
        })

        // 3. sendWhatsAppMessage
        functionDeclarations.put(JSONObject().apply {
            put("name", "sendWhatsAppMessage")
            put("description", "Opens WhatsApp with a pre-filled text message to the specified contact or phone number.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("contactName", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Name of the recipient contact or phone number")
                })
                props.put("message", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "The text message content to send")
                })
                put("properties", props)
                put("required", JSONArray().apply {
                    put("contactName")
                    put("message")
                })
            })
        })

        // 4. sendGmail
        functionDeclarations.put(JSONObject().apply {
            put("name", "sendGmail")
            put("description", "Composes an email via Gmail or email client with recipient, subject, and body.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                val props = JSONObject()
                props.put("recipientEmail", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Email address of the recipient")
                })
                props.put("subject", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Subject line of the email")
                })
                props.put("body", JSONObject().apply {
                    put("type", "STRING")
                    put("description", "Message body of the email")
                })
                put("properties", props)
                put("required", JSONArray().apply {
                    put("recipientEmail")
                    put("subject")
                    put("body")
                })
            })
        })

        // 5. getDeviceInfo
        functionDeclarations.put(JSONObject().apply {
            put("name", "getDeviceInfo")
            put("description", "Retrieves current device status, battery percentage, charging status, and local time.")
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", JSONObject())
            })
        })

        toolObj.put("functionDeclarations", functionDeclarations)
        toolsArray.put(toolObj)
        setup.put("tools", toolsArray)

        root.put("setup", setup)
        return root.toString()
    }

    fun buildAudioInputJson(pcmData: ByteArray, sampleRate: Int = 16000): String {
        val root = JSONObject()
        val realtimeInput = JSONObject()
        val mediaChunks = JSONArray()
        val chunk = JSONObject()
        chunk.put("mimeType", "audio/pcm;rate=$sampleRate")
        chunk.put("data", Base64.encodeToString(pcmData, Base64.NO_WRAP))
        mediaChunks.put(chunk)
        realtimeInput.put("mediaChunks", mediaChunks)
        root.put("realtimeInput", realtimeInput)
        return root.toString()
    }

    fun buildToolResponseJson(callId: String, functionName: String, responseData: JSONObject): String {
        val root = JSONObject()
        val toolResponse = JSONObject()
        val functionResponses = JSONArray()
        val resp = JSONObject()
        resp.put("id", callId)
        resp.put("name", functionName)
        resp.put("response", responseData)
        functionResponses.put(resp)
        toolResponse.put("functionResponses", functionResponses)
        root.put("toolResponse", toolResponse)
        return root.toString()
    }
}

sealed class LiveServerEvent {
    object SetupComplete : LiveServerEvent()
    data class AudioData(val pcmBytes: ByteArray, val mimeType: String) : LiveServerEvent()
    data class TextContent(val text: String) : LiveServerEvent()
    data class ToolCall(val callId: String, val functionName: String, val args: Map<String, Any?>) : LiveServerEvent()
    object Interrupted : LiveServerEvent()
    object TurnComplete : LiveServerEvent()
    data class Error(val message: String) : LiveServerEvent()
}
