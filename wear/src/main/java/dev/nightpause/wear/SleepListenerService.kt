package dev.nightpause.wear

import androidx.health.services.client.PassiveListenerService
import androidx.health.services.client.data.UserActivityState
import androidx.health.services.client.data.UserActivityInfo
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class SleepListenerService : PassiveListenerService() {
    override fun onUserActivityInfoReceived(info: UserActivityInfo) {
        super.onUserActivityInfoReceived(info)
        val asleep = info.userActivityState == UserActivityState.USER_ACTIVITY_ASLEEP
        val changedAt = info.stateChangeTime.toEpochMilli()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val wearable = Wearable.getMessageClient(this@SleepListenerService)
                val bytes = "${if (asleep) "asleep" else "awake"}|$changedAt".toByteArray()
                Wearable.getNodeClient(this@SleepListenerService).connectedNodes.await().forEach { node ->
                    wearable.sendMessage(node.id, "/sleep-state", bytes).await()
                }
            }
        }
    }
}
