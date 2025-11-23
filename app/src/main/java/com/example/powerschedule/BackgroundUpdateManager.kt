package com.example.powerschedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*

class BackgroundUpdateManager {

    companion object {
        private const val UPDATE_INTERVAL = 30 * 60 * 1000L // 30 хвилин

        fun startBackgroundUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, ScheduleUpdateReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)

            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, 30)
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                UPDATE_INTERVAL,
                pendingIntent
            )
        }

        fun stopBackgroundUpdates(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, ScheduleUpdateReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1000,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
        }

        fun checkNow(context: Context) {
            val intent = Intent(context, ScheduleUpdateReceiver::class.java)
            context.sendBroadcast(intent)
        }
    }
}