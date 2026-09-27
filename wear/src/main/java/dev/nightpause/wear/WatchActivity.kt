package dev.nightpause.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.PassiveListenerConfig
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await as taskAwait

class WatchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var enabled by remember { mutableStateOf(false) }
            var loading by remember { mutableStateOf(false) }
            var message by remember { mutableStateOf("Pause media when you fall asleep") }
            val scope = rememberCoroutineScope()

            fun startEnabling() {
                scope.launch {
                    loading = true
                    val error = enableSleepDetection()
                    loading = false
                    enabled = error == null
                    message = error ?: "Your phone will pause playback."
                }
            }

            val permissionRequest = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) startEnabling()
                else message = "Activity access is needed to detect sleep."
            }

            LaunchedEffect(Unit) {
                if (getSharedPreferences("nightpause", MODE_PRIVATE).getBoolean("enabled", false)) {
                    loading = true
                    val error = enableSleepDetection()
                    loading = false
                    enabled = error == null
                    message = error ?: "Your phone will pause playback."
                }
            }

            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                        .animateContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Night Pause", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    AnimatedContent(targetState = when {
                        loading -> "loading"
                        enabled -> "enabled"
                        else -> "off"
                    }, label = "detection-state") { state ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (state) {
                                "loading" -> {
                                    CircularProgressIndicator()
                                    Text("Setting up…", textAlign = TextAlign.Center)
                                }
                                "enabled" -> {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    ) {
                                        Text("✓", color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Text("Sleep detection on", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                                    Text(message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                }
                                else -> {
                                    Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                    Button(onClick = {
                                        if (ContextCompat.checkSelfPermission(
                                                this@WatchActivity,
                                                Manifest.permission.ACTIVITY_RECOGNITION
                                            ) == PackageManager.PERMISSION_GRANTED
                                        ) startEnabling()
                                        else permissionRequest.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                                    }) { Text("Enable") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun enableSleepDetection(): String? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            getSharedPreferences("nightpause", MODE_PRIVATE).edit().putBoolean("enabled", false).apply()
            return "Activity access is needed to detect sleep."
        }
        return runCatching {
            val client = HealthServices.getClient(this).passiveMonitoringClient
            val capabilities = client.getCapabilitiesAsync().await()
            register(client, capabilities)
            getSharedPreferences("nightpause", MODE_PRIVATE).edit().putBoolean("enabled", true).apply()
            reportEnabled()
            null
        }.getOrElse {
            getSharedPreferences("nightpause", MODE_PRIVATE).edit().putBoolean("enabled", false).apply()
            reportEnabled()
            if (it.message?.contains("not supported", ignoreCase = true) == true) {
                "Sleep detection isn’t supported on this watch."
            } else {
                "Couldn’t enable sleep detection. Try again."
            }
        }
    }

    private suspend fun reportEnabled() {
        val isEnabled = getSharedPreferences("nightpause", MODE_PRIVATE).getBoolean("enabled", false)
        val data = PutDataMapRequest.create("/sleep-enabled").apply {
            dataMap.putBoolean("enabled", isEnabled)
            dataMap.putLong("updated_at", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        runCatching { Wearable.getDataClient(this).putDataItem(data).taskAwait() }
        runCatching {
            val payload = byteArrayOf(if (isEnabled) 1 else 0)
            val messages = Wearable.getMessageClient(this)
            Wearable.getNodeClient(this).connectedNodes.taskAwait().forEach { node ->
                messages.sendMessage(node.id, "/sleep-enabled", payload).taskAwait()
            }
        }
    }

    companion object {
        suspend fun register(
            client: androidx.health.services.client.PassiveMonitoringClient,
            capabilities: androidx.health.services.client.data.PassiveMonitoringCapabilities
        ) {
            check(androidx.health.services.client.data.UserActivityState.USER_ACTIVITY_ASLEEP in capabilities.supportedUserActivityStates) {
                "Sleep detection is not supported on this watch"
            }
            val config = PassiveListenerConfig.builder()
                .setShouldUserActivityInfoBeRequested(true)
                .build()
            client.setPassiveListenerServiceAsync(SleepListenerService::class.java, config).await()
        }
    }
}
