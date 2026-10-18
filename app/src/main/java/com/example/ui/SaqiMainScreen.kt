package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import com.example.model.LiveSessionUiState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssistantState
import com.example.model.ConnectionState
import com.example.ui.components.PermissionsOnboardingCard
import com.example.ui.components.SaqiOrbVisualizer
import com.example.ui.components.SaqiSettingsSheet
import com.example.ui.theme.SaqiBackground
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
import com.example.ui.theme.SaqiYellow

@Composable
fun SaqiMainScreen(
    viewModel: SaqiViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "AmbientGlow")
    val ambientAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AmbientPulse"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SaqiBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SaqiPurple.copy(alpha = ambientAlpha * 0.4f),
                            SaqiSurfaceVariant.copy(alpha = 0.2f),
                            SaqiBackground
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {

                // 1. TOP HEADER & BRANDING
                TopBarSection(
                    isBackgroundActive = uiState.isBackgroundServiceRunning,
                    onOpenSettings = { showSettingsSheet = true },
                    onCheckPermissions = { viewModel.checkPermissions() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. PERMISSIONS ONBOARDING (If missing any)
                if (!uiState.hasAllPermissions) {
                    PermissionsOnboardingCard(
                        onPermissionsUpdated = { viewModel.checkPermissions() },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // 3. STATUS BADGE
                StatusIndicatorBadge(
                    connectionState = uiState.connectionState,
                    assistantState = uiState.assistantState
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 4. CENTRAL ANIMATED ORB VISUALIZER
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    SaqiOrbVisualizer(
                        assistantState = uiState.assistantState,
                        connectionState = uiState.connectionState,
                        micInputLevel = uiState.micInputLevel,
                        outputAudioLevel = uiState.outputAudioLevel,
                        onClick = { viewModel.onOrbClicked() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. LIVE ASSISTANT STATUS / VOICE TRANSCRIPT
                VoiceResponseBanner(
                    statusText = uiState.statusText,
                    lastSpoken = uiState.lastSaqiSpeech,
                    assistantState = uiState.assistantState,
                    activeToolName = uiState.activeToolName
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6. BOTTOM CONTROLS & WAKE-WORD STATUS
                BottomQuickControlBar(
                    uiState = uiState,
                    onToggleSession = { viewModel.onOrbClicked() },
                    onOpenSettings = { showSettingsSheet = true }
                )
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SaqiSettingsSheet(
            uiState = uiState,
            currentApiKey = apiKey,
            onSaveApiKey = { viewModel.saveApiKey(it) },
            onSelectVoice = { viewModel.setVoice(it) },
            onSetSensitivity = { viewModel.setWakeWordSensitivity(it) },
            onToggleWakeWord = { viewModel.toggleWakeWord(it) },
            onTestTool = { tool, args -> viewModel.testToolDirectly(tool, args) },
            onDismiss = { showSettingsSheet = false }
        )
    }
}

@Composable
private fun TopBarSection(
    isBackgroundActive: Boolean,
    onOpenSettings: () -> Unit,
    onCheckPermissions: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SAQI",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = SaqiCyan,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SaqiMagenta.copy(alpha = 0.25f))
                        .border(1.dp, SaqiMagenta.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaqiPinkGlow
                    )
                }
            }
            Text(
                text = "Made by Aqeel Saqi",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SaqiTextMuted
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Background service active badge
            if (isBackgroundActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SaqiSurfaceVariant)
                        .border(1.dp, SaqiGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SaqiGreen)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hotword \"SAQI\"",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SaqiGreen
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SaqiSurfaceVariant)
                    .testTag("settings_top_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = SaqiCyan
                )
            }
        }
    }
}

@Composable
private fun StatusIndicatorBadge(
    connectionState: ConnectionState,
    assistantState: AssistantState
) {
    val (label, color) = when (assistantState) {
        AssistantState.SPEAKING -> "SAQI Speaking..." to SaqiPinkGlow
        AssistantState.THINKING -> "SAQI Thinking..." to SaqiYellow
        AssistantState.LISTENING -> "Listening to you..." to SaqiCyan
        AssistantState.IDLE -> when (connectionState) {
            ConnectionState.CONNECTED -> "Ready • Speak or say \"SAQI\"" to SaqiGreen
            ConnectionState.CONNECTING -> "Connecting to Gemini Live..." to SaqiCyan
            ConnectionState.RECONNECTING -> "Reconnecting..." to SaqiYellow
            ConnectionState.ERROR -> "Connection Error" to Color.Red
            ConnectionState.DISCONNECTED -> "Zero-Touch • Tap to Awaken" to SaqiTextMuted
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SaqiSurfaceVariant.copy(alpha = 0.9f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun VoiceResponseBanner(
    statusText: String,
    lastSpoken: String,
    assistantState: AssistantState,
    activeToolName: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("voice_response_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = SaqiSurfaceElevated.copy(alpha = 0.75f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            SaqiPurple.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (activeToolName != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SaqiYellow)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Native Action: $activeToolName",
                        color = SaqiYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = if (lastSpoken.isNotBlank() && assistantState == AssistantState.SPEAKING) {
                    "“$lastSpoken”"
                } else {
                    statusText
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (assistantState == AssistantState.SPEAKING) SaqiTextPrimary else SaqiTextSecondary,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = if (assistantState == AssistantState.SPEAKING) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun BottomQuickControlBar(
    uiState: LiveSessionUiState,
    onToggleSession: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, SaqiCyan.copy(alpha = 0.2f), RoundedCornerShape(22.dp)),
        color = SaqiSurface.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wake word status info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = if (uiState.isWakeWordActive) SaqiCyan else SaqiTextMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Hotword \"SAQI\"",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isWakeWordActive) SaqiCyan else SaqiTextMuted
                    )
                    Text(
                        text = if (uiState.isBackgroundServiceRunning) "Running in background" else "Foreground mode",
                        fontSize = 10.sp,
                        color = SaqiTextMuted
                    )
                }
            }

            // Quick Toggle Button
            val isConnected = uiState.connectionState == ConnectionState.CONNECTED
            IconButton(
                onClick = onToggleSession,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) SaqiMagenta.copy(alpha = 0.25f) else SaqiCyan.copy(alpha = 0.2f)
                    )
                    .testTag("toggle_session_button")
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.PowerSettingsNew else Icons.Default.Mic,
                    contentDescription = if (isConnected) "Disconnect" else "Connect",
                    tint = if (isConnected) SaqiPinkGlow else SaqiCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
