package dev.nightpause.phone

import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class MainActivity : ComponentActivity() {
    private var mediaAccess by mutableStateOf(false)
    private var watchEnabled by mutableStateOf(false)
    private val setupPreferences by lazy { getSharedPreferences("nightpause", 0) }
    private val setupPreferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { preferences, key ->
        if (key == "watch_enabled") watchEnabled = preferences.getBoolean("watch_enabled", false)
    }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); refreshSetup(); setContent { NightPauseScreen(mediaAccess, watchEnabled) } }
    override fun onStart() { super.onStart(); setupPreferences.registerOnSharedPreferenceChangeListener(setupPreferenceListener); refreshSetup() }
    override fun onStop() { setupPreferences.unregisterOnSharedPreferenceChangeListener(setupPreferenceListener); super.onStop() }
    override fun onResume() { super.onResume(); refreshSetup(); refreshWatchStatus() }
    private fun refreshSetup() {
        mediaAccess = hasNotificationAccess(this)
        watchEnabled = setupPreferences.getBoolean("watch_enabled", false)
    }

    private fun refreshWatchStatus() {
        Wearable.getDataClient(this)
            .getDataItems(Uri.parse("wear://*/sleep-enabled"), DataClient.FILTER_PREFIX)
            .addOnSuccessListener { items ->
                try {
                    val current = items.firstOrNull { it.uri.path == "/sleep-enabled" } ?: return@addOnSuccessListener
                    val isEnabled = DataMapItem.fromDataItem(current).dataMap.getBoolean("enabled")
                    setupPreferences.edit().putBoolean("watch_enabled", isEnabled).apply()
                } finally {
                    items.release()
                }
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NightPauseScreen(mediaAccess: Boolean, watchEnabled: Boolean) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("nightpause", 0) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("enabled", true)) }
    var grace by remember { mutableIntStateOf(prefs.getInt("grace", 2)) }
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors) {
        Scaffold(topBar = { TopAppBar(title = { Text("Night Pause", fontWeight = FontWeight.SemiBold) }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Pause on sleep", style = MaterialTheme.typography.titleLarge)
                        Text("Your watch detects when you drift off", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = enabled, onCheckedChange = { enabled = it; prefs.edit().putBoolean("enabled", it).apply() })
                }
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Setup", style = MaterialTheme.typography.titleMedium)
                        SetupRow(R.drawable.ic_phone_android, "Phone access", if (mediaAccess) "Ready" else "Needed")
                        if (!mediaAccess) Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Allow media control") }
                        SetupRow(R.drawable.ic_watch, "Watch", if (watchEnabled) "Sleep detection is on" else "Enable sleep detection on your watch")
                    }
                }
                ElevatedCard {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Pause delay", style = MaterialTheme.typography.titleMedium)
                            Text("${grace} min", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(value = grace.toFloat(), onValueChange = { grace = it.toInt() }, onValueChangeFinished = { prefs.edit().putInt("grace", grace).apply() }, valueRange = 0f..5f, steps = 4)
                    }
                }
                Text("Playback pauses. It won’t resume by itself.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable private fun SetupRow(icon: Int, title: String, detail: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.padding(18.dp).size(24.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column { Text(title, style = MaterialTheme.typography.bodyLarge); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun hasNotificationAccess(context: android.content.Context): Boolean {
    val manager = context.getSystemService(MediaSessionManager::class.java)
    return try { manager.getActiveSessions(ComponentName(context, MediaAccessService::class.java)); true } catch (_: SecurityException) { false }
}
