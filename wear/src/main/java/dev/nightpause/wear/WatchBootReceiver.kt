package dev.nightpause.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.health.services.client.HealthServices
import androidx.work.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.guava.await

class WatchBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        WorkManager.getInstance(context).enqueueUniqueWork("register-sleep-monitoring", ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<RegisterSleepMonitoringWorker>().build())
    }
}

class RegisterSleepMonitoringWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = runCatching {
        runBlocking {
            if (androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACTIVITY_RECOGNITION) != android.content.pm.PackageManager.PERMISSION_GRANTED) return@runBlocking false
            val client = HealthServices.getClient(applicationContext).passiveMonitoringClient
            val capabilities = client.getCapabilitiesAsync().await()
            WatchActivity.register(client, capabilities)
            true
        }
    }.fold({ if (it) Result.success() else Result.failure() }, { Result.retry() })
}
