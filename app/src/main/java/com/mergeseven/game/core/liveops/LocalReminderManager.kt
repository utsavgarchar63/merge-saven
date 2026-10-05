package com.mergeseven.game.core.liveops

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.*
import com.mergeseven.game.MainActivity
import com.mergeseven.game.R
import com.mergeseven.game.data.preferences.SettingsRepository
import com.mergeseven.game.data.repository.UserDataRepository
import com.mergeseven.game.di.PersistenceScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Opt-in daily reminder stored on-device. No token, topics, server or network constraint. */
@Singleton
class LocalReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    @PersistenceScope private val scope: CoroutineScope
) {
    fun syncAsync() { scope.launch { sync() } }
    suspend fun sync() {
        val work = WorkManager.getInstance(context)
        if (!settingsRepository.isNotificationsEnabled.first() || !permissionGranted(context)) {
            work.cancelUniqueWork(WORK)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION)
            return
        }
        val request = PeriodicWorkRequestBuilder<PuzzleReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(ReminderPolicy.delayUntilEvening(ZonedDateTime.now()), TimeUnit.MILLISECONDS)
            .addTag(WORK).build()
        work.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }
    companion object {
        const val WORK = "local_daily_puzzle_reminder"
        const val CHANNEL = "daily_puzzles"
        const val NOTIFICATION = 701
        fun permissionGranted(context: Context) = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderDependencies {
    fun settings(): SettingsRepository
    fun userData(): UserDataRepository
}

class PuzzleReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, ReminderDependencies::class.java)
        if (!deps.settings().isNotificationsEnabled.first() || !LocalReminderManager.permissionGranted(applicationContext))
            return Result.success()
        val now = ZonedDateTime.now()
        val date = now.toLocalDate().toString()
        val prefs = applicationContext.getSharedPreferences("local_reminders", Context.MODE_PRIVATE)
        deps.userData().awaitReady()
        val content = DailyReminderContent.forPlayer(deps.userData().userProfile.value, now.toLocalDate())
            ?: return Result.success()
        val foreground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (!ReminderPolicy.shouldNotify(now.hour, foreground, prefs.getString("last_sent", null) == date,
                false)) return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return Result.success()
        if (Build.VERSION.SDK_INT >= 26) applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(LocalReminderManager.CHANNEL, "Daily puzzle reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "One gentle evening invitation to today's puzzle"; enableVibration(false)
            })
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = "com.mergeseven.game.OPEN_DAILY"
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(applicationContext, 701, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(applicationContext, LocalReminderManager.CHANNEL)
            .setSmallIcon(R.drawable.icon_nav_challenges)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(content.title).bigText(content.body))
            .setColor(android.graphics.Color.rgb(241, 194, 86))
            .addAction(0, content.action, pending)
            .setContentIntent(pending).setAutoCancel(true).setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).build()
        try {
            NotificationManagerCompat.from(applicationContext).notify(LocalReminderManager.NOTIFICATION, notification)
            prefs.edit().putString("last_sent", date).apply()
        } catch (_: SecurityException) { /* Permission may be revoked between the checks and notify. */ }
        return Result.success()
    }
}
