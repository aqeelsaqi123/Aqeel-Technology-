package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.AssistantState
import com.example.model.ConnectionState
import com.example.ui.theme.SaqiCyan
import com.example.ui.theme.SaqiCyanGlow
import com.example.ui.theme.SaqiMagenta
import com.example.ui.theme.SaqiPinkGlow
import com.example.ui.theme.SaqiPurple
import com.example.ui.theme.SaqiViolet
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SaqiOrbVisualizer(
    assistantState: AssistantState,
    connectionState: ConnectionState,
    micInputLevel: Float,
    outputAudioLevel: Float,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbMotion")

    // Slow breathing animation for IDLE
    val breathingPulse = infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Breathing"
    )

    // Fast rotation for THINKING
    val thinkingRotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ThinkingRotation"
    )

    // Reverse rotation for outer orbital ring
    val reverseRotation = infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ReverseRotation"
    )

    // Continuous wave phase for SPEAKING and LISTENING
    val wavePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .testTag("saqi_orb_button")
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.width * 0.28f

            when (assistantState) {
                AssistantState.IDLE -> {
                    val radius = baseRadius * breathingPulse.value

                    // Outer ambient neon glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SaqiCyanGlow.copy(alpha = 0.35f),
                                SaqiMagenta.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.6f
                        ),
                        radius = radius * 1.6f,
                        center = center
                    )

                    // Core glowing orb
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SaqiCyan,
                                SaqiPurple,
                                SaqiSurface
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // Delicate orbital ring
                    drawCircle(
                        color = SaqiCyan.copy(alpha = 0.5f),
                        radius = radius * 1.25f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                AssistantState.LISTENING -> {
                    // React to mic input amplitude
                    val dynamicLevel = (micInputLevel * 1.8f).coerceIn(0f, 1f)
                    val radius = baseRadius * (1f + dynamicLevel * 0.25f)

                    // Multi-layer ripple soundwaves responding to user mic
                    for (i in 1..3) {
                        val ringRadius = radius + (i * 22.dp.toPx() * (0.6f + dynamicLevel * 0.7f))
                        drawCircle(
                            color = SaqiCyan.copy(alpha = (0.45f / i) * (0.5f + dynamicLevel)),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = (2.5f - (i * 0.5f)).dp.toPx())
                        )
                    }

                    // Radiating audio wave bars around the orb
                    val barCount = 36
                    for (b in 0 until barCount) {
                        val angle = (b * (360f / barCount)) * (PI.toFloat() / 180f)
                        val waveOffset = sin(angle * 4 + wavePhase.value) * (8.dp.toPx() + dynamicLevel * 24.dp.toPx())
                        val inner = radius + 4.dp.toPx()
                        val outer = inner + 10.dp.toPx() + waveOffset.coerceAtLeast(0f)

                        val startP = Offset(center.x + cos(angle) * inner, center.y + sin(angle) * inner)
                        val endP = Offset(center.x + cos(angle) * outer, center.y + sin(angle) * outer)

                        drawLine(
                            brush = Brush.linearGradient(listOf(SaqiCyan, SaqiCyanGlow)),
                            start = startP,
                            end = endP,
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Glowing center core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, SaqiCyan, SaqiPurple),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                AssistantState.THINKING -> {
                    val radius = baseRadius * 1.05f

                    // Pulsing background nebula
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SaqiMagenta.copy(alpha = 0.45f),
                                SaqiViolet.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.5f
                        ),
                        radius = radius * 1.5f,
                        center = center
                    )

                    // Spinning orbital arc 1 (Cyan)
                    val arcRadius1 = radius * 1.28f
                    val angleRad1 = thinkingRotation.value * (PI.toFloat() / 180f)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(SaqiCyan, SaqiCyanGlow, Color.Transparent),
                            center = center
                        ),
                        startAngle = thinkingRotation.value,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = Offset(center.x - arcRadius1, center.y - arcRadius1),
                        size = androidx.compose.ui.geometry.Size(arcRadius1 * 2, arcRadius1 * 2),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Counter-spinning orbital arc 2 (Magenta)
                    val arcRadius2 = radius * 1.45f
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(SaqiMagenta, SaqiPinkGlow, Color.Transparent),
                            center = center
                        ),
                        startAngle = reverseRotation.value,
                        sweepAngle = 220f,
                        useCenter = false,
                        topLeft = Offset(center.x - arcRadius2, center.y - arcRadius2),
                        size = androidx.compose.ui.geometry.Size(arcRadius2 * 2, arcRadius2 * 2),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Central energy core
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White, SaqiMagenta, SaqiPurple),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }

                AssistantState.SPEAKING -> {
                    // Fluid, modulated audio wave driven by Gemini Live audio stream
                    val dynamicAudio = (outputAudioLevel * 2f).coerceIn(0.1f, 1f)
                    val radius = baseRadius * (1f + dynamicAudio * 0.2f)

                    // Flowing undulating wave ribbons
                    for (layer in 0..2) {
                        val path = Path()
                        val layerPoints = 48
                        val layerAlpha = 0.7f - (layer * 0.2f)
                        val layerColor = if (layer % 2 == 0) SaqiPinkGlow else SaqiCyanGlow

                        for (p in 0..layerPoints) {
                            val angle = (p * (360f / layerPoints)) * (PI.toFloat() / 180f)
                            val waveR = radius + (layer * 8.dp.toPx()) +
                                    sin(angle * 5 + wavePhase.value * (1.5f + layer * 0.5f)) * (16.dp.toPx() * dynamicAudio)

                            val x = center.x + cos(angle) * waveR
                            val y = center.y + sin(angle) * waveR
                            if (p == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        path.close()

                        drawPath(
                            path = path,
                            color = layerColor.copy(alpha = layerAlpha),
                            style = Stroke(width = (3.5f - layer).dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Rich radial aura
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White,
                                SaqiPinkGlow,
                                SaqiMagenta,
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius * 1.35f
                        ),
                        radius = radius * 1.15f,
                        center = center
                    )
                }
            }
        }

        // Central Icon / Presence Glyphs
        val icon = when (assistantState) {
            AssistantState.IDLE -> if (connectionState == ConnectionState.CONNECTED) Icons.Default.Mic else Icons.Default.MicOff
            AssistantState.LISTENING -> Icons.Default.GraphicEq
            AssistantState.THINKING -> Icons.Default.AutoAwesome
            AssistantState.SPEAKING -> Icons.Default.GraphicEq
        }

        val iconColor = when (assistantState) {
            AssistantState.IDLE -> if (connectionState == ConnectionState.CONNECTED) SaqiCyan else Color.White.copy(alpha = 0.7f)
            AssistantState.LISTENING -> Color.White
            AssistantState.THINKING -> Color.White
            AssistantState.SPEAKING -> Color.White
        }

        Icon(
            imageVector = icon,
            contentDescription = "SAQI Assistant Status: $assistantState",
            tint = iconColor,
            modifier = Modifier.size(54.dp)
        )
    }
}

private val SaqiSurface = Color(0xFF101520)
