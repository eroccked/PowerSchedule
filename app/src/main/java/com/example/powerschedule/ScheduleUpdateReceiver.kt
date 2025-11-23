package com.example.powerschedule

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.net.URL

class ScheduleUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        checkForUpdates(context)
    }

    private fun checkForUpdates(context: Context) {
        val prefs = context.getSharedPreferences("PowerSchedule", Context.MODE_PRIVATE)
        val queueCount = prefs.getInt("queue_count", 0)

        if (queueCount == 0) return

        CoroutineScope(Dispatchers.IO).launch {
            for (i in 0 until queueCount) {
                val queueName = prefs.getString("queue_name_$i", "") ?: ""
                val queueValue = prefs.getString("queue_value_$i", "") ?: ""

                if (queueValue.isNotEmpty()) {
                    checkQueueForChanges(context, queueName, queueValue, i)
                }
            }
        }
    }

    private suspend fun checkQueueForChanges(
        context: Context,
        queueName: String,
        queueValue: String,
        index: Int
    ) {
        try {
            val prefs = context.getSharedPreferences("PowerSchedule", Context.MODE_PRIVATE)

            val autoUpdateEnabled = prefs.getBoolean("auto_update_$queueValue", true)
            if (!autoUpdateEnabled) {
                return
            }

            val url = "https://be-svitlo.oe.if.ua/schedule-by-queue?queue=$queueValue"
            val jsonString = URL(url).readText()

            val savedSchedule = prefs.getString("saved_schedule_$index", "")

            if (savedSchedule != jsonString) {
                prefs.edit().putString("saved_schedule_$index", jsonString).apply()

                if (!savedSchedule.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) {
                        showChangeNotification(context, queueName)
                        updateWidget(context)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showChangeNotification(context: Context, queueName: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "power_schedule_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📊 Графік оновлено!")
            .setContentText("Графік для \"$queueName\" змінився. Натисніть для перегляду.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        notificationManager.notify(2001 + queueName.hashCode(), notification)
    }

    private fun updateWidget(context: Context) {
        val intent = Intent(context, PowerScheduleWidget::class.java)
        intent.action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        context.sendBroadcast(intent)
    }
}