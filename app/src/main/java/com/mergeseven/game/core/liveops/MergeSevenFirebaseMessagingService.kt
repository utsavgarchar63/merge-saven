package com.mergeseven.game.core.liveops

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.mergeseven.game.MainActivity
import com.mergeseven.game.R
import com.mergeseven.game.data.preferences.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * FCM entry point (AF6-07). Honours settings opt-in and runtime notification permission.
 */
@AndroidEntryPoint
class MergeSevenFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var pushTopicManager: PushTopicManager

    override fun onNewToken(token: String) {
        pushTopicManager.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val type = message.data["type"] ?: message.data["event"] ?: return
        if (type !in ALLOWED_TYPES) return
        val optedIn = runBlocking { settingsRepository.isNotificationsEnabled.first() }
        if (!optedIn) return
        if (!hasPostPermission()) return

        val title = message.data["title"]
            ?: message.notification?.title
            ?: defaultTitle(type)
        val body = message.data["body"]
            ?: message.notification?.body
            ?: defaultBody(type)
        showNotification(type, title, body)
    }

    private fun hasPostPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun showNotification(type: String, title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Merge Seven",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_PUSH_TYPE, type)
        }
        val pending = PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_play)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        manager.notify(type.hashCode(), notification)
    }

    private fun defaultTitle(type: String): String = when (type) {
        TYPE_DAILY -> "Daily puzzle"
        TYPE_STREAK -> "Streak at risk"
        TYPE_EVENT -> "Event started"
        else -> "Merge Seven"
    }

    private fun defaultBody(type: String): String = when (type) {
        TYPE_DAILY -> "Your daily challenge is ready."
        TYPE_STREAK -> "Play today to keep your streak."
        TYPE_EVENT -> "A seasonal event is live — jump in."
        else -> "Something new is waiting."
    }

    companion object {
        const val CHANNEL_ID = "merge_seven_liveops"
        const val EXTRA_PUSH_TYPE = "push_type"
        const val TYPE_DAILY = "daily_reminder"
        const val TYPE_STREAK = "streak_at_risk"
        const val TYPE_EVENT = "event_start"
        private val ALLOWED_TYPES = setOf(TYPE_DAILY, TYPE_STREAK, TYPE_EVENT)
    }
}
