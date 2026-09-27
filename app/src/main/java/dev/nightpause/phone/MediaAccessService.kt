package dev.nightpause.phone

import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MediaAccessService : NotificationListenerService() {
    override fun onListenerConnected() { super.onListenerConnected(); instance = this }
    override fun onListenerDisconnected() { instance = null; super.onListenerDisconnected() }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { instance = this }

    fun pausePlayingMedia(): Boolean {
        val sessions = getSystemService(MediaSessionManager::class.java).getActiveSessions(android.content.ComponentName(this, MediaAccessService::class.java))
        val active = sessions.filter { it.playbackState?.state in PLAYING_STATES }
        active.forEach { it.transportControls.pause() }
        return active.isNotEmpty()
    }

    companion object {
        @Volatile var instance: MediaAccessService? = null
        private val PLAYING_STATES = setOf(android.media.session.PlaybackState.STATE_PLAYING, android.media.session.PlaybackState.STATE_BUFFERING, android.media.session.PlaybackState.STATE_CONNECTING)
    }
}
