package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LiveSessionUiState
import com.example.ui.theme.SaqiCyan
import com.example.ui.theme.SaqiCyanGlow
import com.example.ui.theme.SaqiGreen
import com.example.ui.theme.SaqiMagenta
import com.example.ui.theme.SaqiPinkGlow
import com.example.ui.theme.SaqiPurple
import com.example.ui.theme.SaqiSurface
import com.example.ui.theme.SaqiSurfaceElevated
import com.example.ui.theme.SaqiSurfaceVariant
import com.example.ui.theme.SaqiTextMuted
import com.example.ui.theme.SaqiTextPrimary
import com.example.ui.theme.SaqiTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaqiSettingsSheet(
    uiState: LiveSessionUiState,
    currentApiKey: String,
    onSaveApiKey: (String) -> Unit,
    onSelectVoice: (String) -> Unit,
    onSetSensitivity: (Float) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onTestTool: (String, Map<String, Any?>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var apiKeyInput by remember { mutableStateOf(currentApiKey) }
    var sensitivityInput by remember { mutableFloatStateOf(uiState.wakeWordSensitivity) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SaqiSurface,
        scrimColor = Color.Black.copy(alpha = 0.75f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(SaqiTextMuted.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SAQI Settings & Tools",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SaqiTextPrimary
                    )
                    Text(
                        text = "Customize persona, voice & device tools",
                        style = MaterialTheme.typography.bodySmall,
                        color = SaqiCyanGlow
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_settings_button")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = SaqiTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 1. Gemini Live API Key Section
            SettingsSectionHeader(icon = Icons.Default.Key, title = "Gemini Live API Key")
            Text(
                text = "Used for continuous bi-directional voice streaming. Injected automatically in AI Studio from Secrets panel, or enter your key below.",
                style = MaterialTheme.typography.bodySmall,
                color = SaqiTextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = { apiKeyInput = it },
                label = { Text("Gemini API Key") },
                placeholder = { Text("AIzaSy...") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input"),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SaqiCyan,
                    unfocusedBorderColor = SaqiTextMuted.copy(alpha = 0.5f),
                    focusedTextColor = SaqiTextPrimary,
                    unfocusedTextColor = SaqiTextPrimary,
                    focusedContainerColor = SaqiSurfaceVariant,
                    unfocusedContainerColor = SaqiSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onSaveApiKey(apiKeyInput) },
                modifier = Modifier
                    .align(Alignment.End)
                    .testTag("save_api_key_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaqiCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Key", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Voice Selection Section
            SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "Voice Persona")
            Text(
                text = "Pick the voice that matches SAQI's vibrant personality.",
                style = MaterialTheme.typography.bodySmall,
                color = SaqiTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))
            val voices = listOf(
                "Aoede" to "Sassy, bright & witty (Recommended)",
                "Kore" to "Confident, bold & stylish",
                "Puck" to "Playful, energetic banter",
                "Fenrir" to "Deep & resonant"
            )

            voices.forEach { (voice, description) ->
                val isSelected = uiState.currentVoice.equals(voice, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) SaqiSurfaceElevated else SaqiSurfaceVariant)
                        .border(
                            1.dp,
                            if (isSelected) SaqiMagenta else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectVoice(voice) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) SaqiMagenta else SaqiTextMuted)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = voice,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SaqiCyan else SaqiTextPrimary,
                            fontSize = 15.sp
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = SaqiTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Wake-Word & Background Service Section
            SettingsSectionHeader(icon = Icons.Default.Sensors, title = "Wake-Word (\"SAQI\") & Background Service")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SaqiSurfaceVariant)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Listen for \"SAQI\" in Background",
                        fontWeight = FontWeight.SemiBold,
                        color = SaqiTextPrimary
                    )
                    Text(
                        text = "Keeps persistent foreground service active to awaken instantly on wake-word.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SaqiTextMuted,
                        fontSize = 11.sp
                    )
                }
                Switch(
                    checked = uiState.isWakeWordActive,
                    onCheckedChange = { onToggleWakeWord(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = SaqiCyan
                    ),
                    modifier = Modifier.testTag("wake_word_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Wake-Word Sensitivity: ${(sensitivityInput * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = SaqiTextSecondary
            )
            Slider(
                value = sensitivityInput,
                onValueChange = {
                    sensitivityInput = it
                    onSetSensitivity(it)
                },
                valueRange = 0.2f..0.9f,
                colors = SliderDefaults.colors(
                    thumbColor = SaqiCyan,
                    activeTrackColor = SaqiCyanGlow,
                    inactiveTrackColor = SaqiSurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Test Native Tools Section
            SettingsSectionHeader(icon = Icons.Default.Launch, title = "Test Native Device Tools")
            Text(
                text = "Tap any tool to test SAQI's native Android action execution:",
                style = MaterialTheme.typography.bodySmall,
                color = SaqiTextMuted
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolTestButton(
                    modifier = Modifier.weight(1f),
                    label = "YouTube",
                    icon = Icons.Default.Launch,
                    onClick = {
                        onTestTool("openApp", mapOf("appName" to "YouTube"))
                    }
                )
                ToolTestButton(
                    modifier = Modifier.weight(1f),
                    label = "WhatsApp",
                    icon = Icons.Default.Message,
                    onClick = {
                        onTestTool(
                            "sendWhatsAppMessage",
                            mapOf("contactName" to "Mom", "message" to "Hey darling, sent with love by SAQI!")
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolTestButton(
                    modifier = Modifier.weight(1f),
                    label = "Dial Call",
                    icon = Icons.Default.Call,
                    onClick = {
                        onTestTool("searchAndCallContact", mapOf("contactName" to "Mom"))
                    }
                )
                ToolTestButton(
                    modifier = Modifier.weight(1f),
                    label = "Draft Email",
                    icon = Icons.Default.Email,
                    onClick = {
                        onTestTool(
                            "sendGmail",
                            mapOf(
                                "recipientEmail" to "aqeelhaidermah@gmail.com",
                                "subject" to "Greetings from SAQI",
                                "body" to "Hello! This email was prepared by your sassy assistant SAQI."
                            )
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            ToolTestButton(
                modifier = Modifier.fillMaxWidth(),
                label = "Check Battery & Device Info",
                icon = Icons.Default.Devices,
                onClick = {
                    onTestTool("getDeviceInfo", emptyMap())
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Footer author
            Text(
                text = "SAQI Assistant • Made by Aqeel Saqi",
                style = MaterialTheme.typography.bodySmall,
                color = SaqiTextMuted,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = SaqiCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = SaqiTextPrimary
        )
    }
}

@Composable
private fun ToolTestButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SaqiSurfaceElevated,
            contentColor = SaqiTextPrimary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = SaqiCyan)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
