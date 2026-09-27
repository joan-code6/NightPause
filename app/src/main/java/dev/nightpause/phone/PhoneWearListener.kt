package dev.nightpause.phone

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import androidx.work.*
import java.util.concurrent.TimeUnit

class PhoneWearListener : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED || event.dataItem.uri.path != "/sleep-enabled") return@forEach
            val isEnabled = runCatching {
                DataMapItem.fromDataItem(event.dataItem).dataMap.getBoolean("enabled")
            }.getOrDefault(false)
            getSharedPreferences("nightpause", 0).edit().putBoolean("watch_enabled", isEnabled).apply()
        }
        dataEvents.release()
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == "/sleep-enabled") {
            getSharedPreferences("nightpause", 0).edit()
                .putBoolean("watch_enabled", event.data.firstOrNull() == 1.toByte())
                .apply()
            return
        }
        if (event.path != "/sleep-state") return
        val payload = runCatching { String(event.data).split("|") }.getOrNull() ?: return
        if (payload.size != 2) return
        val asleep = payload[0] == "asleep"
        val detectedAt = payload[1].toLongOrNull() ?: return
        val prefs = getSharedPreferences("nightpause", 0)
        val lastStateAt = prefs.getLong("last_state_at", 0L)
        if (detectedAt <= lastStateAt) return
        prefs.edit().putLong("last_state_at", detectedAt).apply()
        if (!asleep) {
            WorkManager.getInstance(this).cancelUniqueWork("pause-after-sleep")
            return
        }
        if (System.currentTimeMillis() - detectedAt !in 0..(10 * 60 * 1000L)) return
        if (!prefs.getBoolean("enabled", true)) return
        val grace = prefs.getInt("grace", 2).coerceIn(0, 5)
        val request = OneTimeWorkRequestBuilder<PauseMediaWorker>()
            .setInitialDelay(grace.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf("sleep_detected_at" to detectedAt))
            .build()
        WorkManager.getInstance(this).enqueueUniqueWork("pause-after-sleep", ExistingWorkPolicy.REPLACE, request)
    }
}

class PauseMediaWorker(context: android.content.Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (!applicationContext.getSharedPreferences("nightpause", 0).getBoolean("enabled", true)) return Result.success()
        val detectedAt = inputData.getLong("sleep_detected_at", 0L)
        if (detectedAt == 0L || System.currentTimeMillis() - detectedAt > 20 * 60_000L) return Result.success()
        val paused = runCatching { MediaAccessService.instance?.pausePlayingMedia() ?: false }.getOrDefault(false)
        Log.i("NightPause", if (paused) "Paused active media after sleep detection" else "No active media session to pause")
        return Result.success()
    }
}
