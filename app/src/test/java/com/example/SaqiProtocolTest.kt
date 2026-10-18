package com.example

import com.example.model.LiveProtocol
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SaqiProtocolTest {

    @Test
    fun testSetupJsonGeneration() {
        val setupJsonStr = LiveProtocol.buildSetupJson(voiceName = "Aoede")
        val json = JSONObject(setupJsonStr)

        assertTrue("Root must have setup object", json.has("setup"))
        val setup = json.getJSONObject("setup")

        assertEquals("models/gemini-2.0-flash-exp", setup.getString("model"))

        val generationConfig = setup.getJSONObject("generationConfig")
        val responseModalities = generationConfig.getJSONArray("responseModalities")
        assertEquals("AUDIO", responseModalities.getString(0))

        val voiceConfig = generationConfig.getJSONObject("speechConfig").getJSONObject("voiceConfig")
        assertEquals("Aoede", voiceConfig.getJSONObject("prebuiltVoiceConfig").getString("voiceName"))

        val tools = setup.getJSONArray("tools")
        assertTrue("Must have tools array", tools.length() > 0)

        val funcDecls = tools.getJSONObject(0).getJSONArray("functionDeclarations")
        val funcNames = mutableListOf<String>()
        for (i in 0 until funcDecls.length()) {
            funcNames.add(funcDecls.getJSONObject(i).getString("name"))
        }

        assertTrue("Must declare openApp tool", funcNames.contains("openApp"))
        assertTrue("Must declare searchAndCallContact tool", funcNames.contains("searchAndCallContact"))
        assertTrue("Must declare sendWhatsAppMessage tool", funcNames.contains("sendWhatsAppMessage"))
        assertTrue("Must declare sendGmail tool", funcNames.contains("sendGmail"))
        assertTrue("Must declare getDeviceInfo tool", funcNames.contains("getDeviceInfo"))
    }

    @Test
    fun testAudioInputJsonGeneration() {
        val testBytes = byteArrayOf(0, 1, 2, 3, 4, 5)
        val jsonStr = LiveProtocol.buildAudioInputJson(testBytes, 16000)
        val json = JSONObject(jsonStr)

        assertTrue(json.has("realtimeInput"))
        val chunks = json.getJSONObject("realtimeInput").getJSONArray("mediaChunks")
        assertEquals(1, chunks.length())
        assertEquals("audio/pcm;rate=16000", chunks.getJSONObject(0).getString("mimeType"))
        assertNotNull(chunks.getJSONObject(0).getString("data"))
    }

    @Test
    fun testToolResponseJsonGeneration() {
        val respData = JSONObject().apply {
            put("output", "Opened YouTube successfully")
        }
        val jsonStr = LiveProtocol.buildToolResponseJson("call_999", "openApp", respData)
        val json = JSONObject(jsonStr)

        assertTrue(json.has("toolResponse"))
        val responses = json.getJSONObject("toolResponse").getJSONArray("functionResponses")
        assertEquals(1, responses.length())
        assertEquals("call_999", responses.getJSONObject(0).getString("id"))
        assertEquals("openApp", responses.getJSONObject(0).getString("name"))
    }
}
