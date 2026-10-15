package net.utgardnet.downsizing.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Entirely on-device reminder. Runs approximately daily, not at an exact clock time. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()
        manager.createNotificationChannel(
            NotificationChannel("downsizing_daily", "Daily check-in", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val notification = android.app.Notification.Builder(applicationContext, "downsizing_daily")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Downsizing")
            .setContentText("A small check-in: log today's meals, movement, and weight if you wish.")
            .setAutoCancel(true)
            .build()
        manager.notify(1001, notification)
        return Result.success()
    }
}

object Reminders {
    fun setEnabled(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (enabled) {
            manager.enqueueUniquePeriodicWork(
                "downsizing_daily",
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS).build()
            )
        } else {
            manager.cancelUniqueWork("downsizing_daily")
        }
    }
}
