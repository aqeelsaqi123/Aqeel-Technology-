package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.SaqiCyan
import com.example.ui.theme.SaqiCyanGlow
import com.example.ui.theme.SaqiGreen
import com.example.ui.theme.SaqiMagenta
import com.example.ui.theme.SaqiPinkGlow
import com.example.ui.theme.SaqiSurface
import com.example.ui.theme.SaqiSurfaceElevated
import com.example.ui.theme.SaqiSurfaceVariant
import com.example.ui.theme.SaqiTextMuted
import com.example.ui.theme.SaqiTextPrimary
import com.example.ui.theme.SaqiTextSecondary

@Composable
fun PermissionsOnboardingCard(
    onPermissionsUpdated: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    }

    var permissionsState by remember {
        mutableStateOf(checkAllPermissions(context, requiredPermissions))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsState = checkAllPermissions(context, requiredPermissions)
        onPermissionsUpdated()
    }

    val allGranted = permissionsState.values.all { it }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("permissions_onboarding_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = SaqiSurfaceVariant.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (allGranted) SaqiGreen.copy(alpha = 0.4f) else SaqiCyan.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (allGranted) SaqiGreen.copy(alpha = 0.2f) else SaqiMagenta.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (allGranted) Icons.Default.Check else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (allGranted) SaqiGreen else SaqiMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (allGranted) "SAQI is fully unleashed!" else "Superpowers & Guardrails",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SaqiTextPrimary
                        )
                        Text(
                            text = if (allGranted) "All permissions granted. I can call, text & listen!" else "Grant permissions so I can control your device hands-free",
                            style = MaterialTheme.typography.bodySmall,
                            color = SaqiTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Permission checklist
            PermissionItem(
                icon = Icons.Default.Mic,
                title = "Microphone",
                subtitle = "For Gemini Live audio streaming & 'SAQI' wake-word",
                isGranted = permissionsState[Manifest.permission.RECORD_AUDIO] == true
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionItem(
                icon = Icons.Default.Contacts,
                title = "Contacts",
                subtitle = "To find people for WhatsApp messages and calls",
                isGranted = permissionsState[Manifest.permission.READ_CONTACTS] == true
            )

            Spacer(modifier = Modifier.height(10.dp))

            PermissionItem(
                icon = Icons.Default.Call,
                title = "Phone Calls",
                subtitle = "To place phone calls when you tell SAQI to call",
                isGranted = permissionsState[Manifest.permission.CALL_PHONE] == true
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(10.dp))
                PermissionItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Keeps SAQI alive in the background for wake-word",
                    isGranted = permissionsState[Manifest.permission.POST_NOTIFICATIONS] == true
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!allGranted) {
                Button(
                    onClick = { launcher.launch(requiredPermissions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_permissions_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaqiCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "Grant All Permissions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            } else {
                Text(
                    text = "“You gave me the keys to the castle, darling. Ask me anything!”",
                    style = MaterialTheme.typography.bodySmall,
                    color = SaqiCyanGlow,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SaqiSurface.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) SaqiCyan else SaqiTextMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SaqiTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SaqiTextMuted,
                fontSize = 11.sp
            )
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Shield,
            contentDescription = if (isGranted) "Granted" else "Needed",
            tint = if (isGranted) SaqiGreen else SaqiTextMuted,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun checkAllPermissions(context: Context, permissions: Array<String>): Map<String, Boolean> {
    return permissions.associateWith { perm ->
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }
}
