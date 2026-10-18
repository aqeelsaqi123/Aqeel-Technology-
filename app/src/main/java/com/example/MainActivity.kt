package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.service.SaqiBackgroundService
import com.example.ui.SaqiMainScreen
import com.example.ui.SaqiViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SaqiViewModel by viewModels()

    private val wakeWordReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SaqiBackgroundService.ACTION_WAKE_WORD_TRIGGERED) {
                viewModel.triggerFromWakeWord()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        val filter = IntentFilter(SaqiBackgroundService.ACTION_WAKE_WORD_TRIGGERED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeWordReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(wakeWordReceiver, filter)
        }

        setContent {
            MyApplicationTheme {
                SaqiMainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == SaqiBackgroundService.ACTION_WAKE_WORD_TRIGGERED) {
            viewModel.triggerFromWakeWord()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(wakeWordReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
