package com.example.powerschedule

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.*

class NotificationHelper {

    companion object {
        private const val CHANNEL_ID = "power_schedule_channel"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Графік Світла"
                val descriptionText = "Нагадування про відключення світла"
                val importance = NotificationManager.IMPORTANCE_HIGH

                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun scheduleNotification(
            context: Context,
            time: String,
            queueName: String
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val parts = time.split(":")
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                add(Calendar.MINUTE, -30)
            }

            if (calendar.timeInMillis < System.currentTimeMillis()) {
                return
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("queue_name", queueName)
                putExtra("shutdown_time", time)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                time.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        fun cancelAllNotifications(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val queueName = intent.getStringExtra("queue_name") ?: "Ваша черга"
        val shutdownTime = intent.getStringExtra("shutdown_time") ?: ""

        showNotification(context, queueName, shutdownTime)
    }

    private fun showNotification(context: Context, queueName: String, time: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "power_schedule_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚡ Скоро відключення!")
            .setContentText("$queueName: відключення о $time (через 30 хв)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(1001, notification)
    }
}